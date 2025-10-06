
using IdleLoadsheddingAPI.Data;
using IdleLoadsheddingAPI.Models.Entities;
using IdleLoadsheddingAPI.Services.Interfaces;
using Microsoft.EntityFrameworkCore;

namespace IdleLoadsheddingAPI.Services.Implementations;

public class AuthService : IAuthService
{
    private static readonly char[] FriendCodeAlphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".ToCharArray();
    private readonly ApplicationDbContext _dbContext;
    private readonly ILogger<AuthService> _logger;

    public AuthService(ApplicationDbContext dbContext, ILogger<AuthService> logger)
    {
        _dbContext = dbContext;
        _logger = logger;
    }

    public async Task<Player> EnsurePlayerAsync(string firebaseUid, string? email, string? displayName, CancellationToken cancellationToken = default)
    {
        var player = await _dbContext.Players
            .Include(p => p.SaveState)
            .FirstOrDefaultAsync(p => p.PlayerId == firebaseUid, cancellationToken);

        if (player != null)
        {
            bool mutated = false;

            if (!string.IsNullOrWhiteSpace(displayName) && !string.Equals(player.DisplayName, displayName, StringComparison.Ordinal))
            {
                player.DisplayName = displayName!;
                mutated = true;
            }

            if (!string.IsNullOrWhiteSpace(email) && !string.Equals(player.Email, email, StringComparison.OrdinalIgnoreCase))
            {
                player.Email = email;
                mutated = true;
            }

            if (mutated)
            {
                player.UpdatedAt = DateTime.UtcNow;
                await _dbContext.SaveChangesAsync(cancellationToken);
            }

            return player;
        }

        player = new Player
        {
            PlayerId = firebaseUid,
            Email = email,
            DisplayName = !string.IsNullOrWhiteSpace(displayName) ? displayName! : email ?? $"Player-{firebaseUid[..Math.Min(6, firebaseUid.Length)]}",
            FriendCode = await GenerateFriendCodeAsync(cancellationToken),
            CreatedAt = DateTime.UtcNow,
            UpdatedAt = DateTime.UtcNow,
            PrestigeCurrency = 0,
            PrestigeLevel = 0,
            TotalMoneyEarned = 0
        };

        _dbContext.Players.Add(player);
        _dbContext.PlayerSaveStates.Add(new PlayerSaveState
        {
            PlayerId = player.PlayerId,
            StatsJson = "{}",
            CreatedAt = DateTime.UtcNow,
            TimestampUtc = DateTime.UtcNow
        });

        await _dbContext.SaveChangesAsync(cancellationToken);
        _logger.LogInformation("Created new player {@PlayerId}", player.PlayerId);
        return player;
    }

    private async Task<string> GenerateFriendCodeAsync(CancellationToken cancellationToken)
    {
        var random = Random.Shared;
        for (var attempt = 0; attempt < 10; attempt++)
        {
            var code = new string(Enumerable.Range(0, 6).Select(_ => FriendCodeAlphabet[random.Next(FriendCodeAlphabet.Length)]).ToArray());
            var clash = await _dbContext.Players.AnyAsync(p => p.FriendCode == code, cancellationToken);
            if (!clash)
            {
                return code;
            }
        }

        // Fallback to GUID chunk
        return Guid.NewGuid().ToString("N")[..8].ToUpperInvariant();
    }
}
