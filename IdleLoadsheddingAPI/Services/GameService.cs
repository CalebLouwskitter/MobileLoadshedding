using System;
using IdleLoadsheddingAPI.Data;
using IdleLoadsheddingAPI.Models.Requests;
using IdleLoadsheddingAPI.Models.Responses;
using Microsoft.EntityFrameworkCore;
using System.Globalization;
using System.Text.Json;
using IdleLoadsheddingAPI.Models.AppEntities;

namespace IdleLoadsheddingAPI.Services;

public class GameService(GameDbContext context, LeaderboardService leaderboardService)
{
    private readonly GameDbContext _context = context;
    private readonly LeaderboardService _leaderboardService = leaderboardService;
    private static readonly JsonSerializerOptions SnapshotSerializerOptions = new(JsonSerializerDefaults.General)
    {
        PropertyNamingPolicy = JsonNamingPolicy.CamelCase,
        WriteIndented = false
    };

    public async Task<SyncResponse> SyncPlayerStateAsync(
        string firebaseUid,
        SyncRequest request,
        string? email = null,
        string? displayName = null)
    {
        var user = await GetOrCreateUserAsync(firebaseUid, email, displayName, includeNeighbourhoods: true);

        var lastSync = user.LastSyncAt ?? user.CreatedAt;
        var now = DateTime.UtcNow;
        var timeOffline = now - lastSync;
        var offlineIncome = (decimal)timeOffline.TotalMinutes * user.Electricity * 0.1m;

        user.Money += offlineIncome + request.MoneyEarned;
        user.Electricity = Math.Max(0, user.Electricity + request.ElectricityGenerated);
        user.LastSyncAt = now;
        user.LastLoginAt = now;
        user.PrestigeLevel = Math.Max(user.PrestigeLevel, request.PrestigeLevel);

        await _context.SaveChangesAsync();

        // Record leaderboard score based on total wealth and electricity
        var score = user.Money + (user.Electricity * 10);
        await _leaderboardService.RecordScoreAsync(user.UserId, score);

        return new SyncResponse
        {
            TotalMoney = user.Money,
            OfflineEarnings = offlineIncome,
            ElectricityRate = user.Electricity,
            PrestigeLevel = user.PrestigeLevel
        };
    }

    public async Task<UpgradeResponse> ApplyUpgradeAsync(
        string firebaseUid,
        UpgradeRequest request,
        string? email = null,
        string? displayName = null)
    {
        var user = await GetOrCreateUserAsync(firebaseUid, email, displayName);

        if (user.Money < request.Cost)
        {
            throw new InvalidOperationException("Insufficient funds for upgrade.");
        }

        user.Money -= request.Cost;
        user.Electricity += request.ElectricityBoost;

        var upgrade = new Upgrade
        {
            UserId = user.UserId,
            Name = request.Name,
            Cost = request.Cost,
            ElectricityBoost = request.ElectricityBoost,
            PurchasedAt = DateTime.UtcNow
        };

        _context.Upgrades.Add(upgrade);
        await _context.SaveChangesAsync();

        // Update leaderboard after purchase
        var score = user.Money + (user.Electricity * 10);
        await _leaderboardService.RecordScoreAsync(user.UserId, score);

        return new UpgradeResponse
        {
            Success = true,
            NewElectricityRate = user.Electricity,
            RemainingMoney = user.Money
        };
    }

    public async Task RecordLeaderboardScoreAsync(
        string firebaseUid,
        decimal score,
        string? displayName = null)
    {
        var user = await GetOrCreateUserAsync(firebaseUid, null, displayName);
        await _leaderboardService.RecordScoreAsync(user.UserId, score, displayName);
    }

    public async Task RecordStatsSnapshotAsync(
        string firebaseUid,
        StatsSnapshotRequest request,
        string? email = null,
        string? displayName = null)
    {
        var user = await GetOrCreateUserAsync(firebaseUid, email, displayName);

        var payloadJson = JsonSerializer.Serialize(request, SnapshotSerializerOptions);

        var snapshot = new StatsSnapshot
        {
            UserId = user.UserId,
            PayloadJson = payloadJson,
            RecordedAtUtc = ParseTimestampOrNow(request.TimestampUtc)
        };

        _context.StatsSnapshots.Add(snapshot);

        // Update user top-level values for quick access/reporting
        user.Money = (decimal)Math.Max(0, request.CurrentMoney);
        user.Electricity = (decimal)Math.Max(0, request.EnergyPerSecond);
        user.LastSyncAt = snapshot.RecordedAtUtc;

        await _context.SaveChangesAsync();
    }

    public async Task RecordSaveSnapshotAsync(
        string firebaseUid,
        SaveSyncRequest request,
        string? email = null,
        string? displayName = null)
    {
        var user = await GetOrCreateUserAsync(firebaseUid, email, displayName);

        var payloadJson = JsonSerializer.Serialize(request, SnapshotSerializerOptions);

        var snapshot = new SaveSnapshot
        {
            UserId = user.UserId,
            PayloadJson = payloadJson,
            RecordedAtUtc = ParseTimestampOrNow(request.TimestampUtc)
        };

        _context.SaveSnapshots.Add(snapshot);
        await _context.SaveChangesAsync();
    }

    private async Task<User> GetOrCreateUserAsync(
        string firebaseUid,
        string? email,
        string? displayName,
        bool includeNeighbourhoods = false)
    {
        IQueryable<User> query = _context.Users;

        if (includeNeighbourhoods)
        {
            query = query.Include(u => u.Neighbourhoods);
        }

        var user = await query.FirstOrDefaultAsync(u => u.FirebaseUid == firebaseUid);

        if (user is not null)
        {
            // Keep user details fresh if identity data has changed
            var resolvedEmail = ResolveEmail(email, firebaseUid, user.Email);
            var resolvedUsername = ResolveUsername(displayName, resolvedEmail, user.Username);

            if (!string.Equals(user.Email, resolvedEmail, StringComparison.OrdinalIgnoreCase) ||
                !string.Equals(user.Username, resolvedUsername, StringComparison.Ordinal))
            {
                user.Email = resolvedEmail;
                user.Username = resolvedUsername;
                user.LastLoginAt = DateTime.UtcNow;
                await _context.SaveChangesAsync();
            }

            return user;
        }

        var now = DateTime.UtcNow;
        var newEmail = ResolveEmail(email, firebaseUid);
        var newUsername = ResolveUsername(displayName, newEmail, firebaseUid);

        user = new User
        {
            FirebaseUid = firebaseUid,
            Username = newUsername,
            Email = newEmail,
            Money = 0,
            Electricity = 0,
            PrestigeLevel = 0,
            CreatedAt = now,
            LastSyncAt = now,
            LastLoginAt = now
        };

        _context.Users.Add(user);
        await _context.SaveChangesAsync();

        if (includeNeighbourhoods)
        {
            // ensure navigation property is initialised for callers expecting non-null collection
            await _context.Entry(user).Collection(u => u.Neighbourhoods).LoadAsync();
        }

        return user;
    }

    private static string ResolveEmail(string? email, string firebaseUid, string? fallback = null)
    {
        if (!string.IsNullOrWhiteSpace(email))
        {
            return email;
        }

        if (!string.IsNullOrWhiteSpace(fallback))
        {
            return fallback;
        }

        return $"{firebaseUid}@players.idle";
    }

    private static string ResolveUsername(string? displayName, string email, string fallback)
    {
        var candidate = !string.IsNullOrWhiteSpace(displayName)
            ? displayName
            : (!string.IsNullOrWhiteSpace(email)
                ? email.Split('@')[0]
                : fallback);

        candidate = candidate.Trim();

        if (candidate.Length > 50)
        {
            candidate = candidate[..50];
        }

        return string.IsNullOrWhiteSpace(candidate) ? fallback[..Math.Min(fallback.Length, 50)] : candidate;
    }

    private static DateTime ParseTimestampOrNow(string? timestampUtc)
    {
        if (!string.IsNullOrWhiteSpace(timestampUtc) &&
            DateTime.TryParse(timestampUtc, CultureInfo.InvariantCulture, DateTimeStyles.AdjustToUniversal | DateTimeStyles.AssumeUniversal, out var parsed))
        {
            return DateTime.SpecifyKind(parsed, DateTimeKind.Utc);
        }

        return DateTime.UtcNow;
    }
}
