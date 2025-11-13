using IdleLoadsheddingAPI.Data;
using IdleLoadsheddingAPI.Models.AppEntities;
using IdleLoadsheddingAPI.Models.Responses;
using Microsoft.EntityFrameworkCore;

namespace IdleLoadsheddingAPI.Services;

public class LeaderboardService(GameDbContext context)
{
    private readonly GameDbContext _context = context;

    public async Task<IReadOnlyList<LeaderboardEntryResponse>> GetTopEntriesAsync(int take)
    {
        return await _context.LeaderboardEntries
            .AsNoTracking()
            .OrderByDescending(entry => entry.Score)
            .ThenBy(entry => entry.RecordedAt)
            .Take(take)
            .Select(entry => new LeaderboardEntryResponse
            {
                PlayerId = entry.UserId,
                Username = entry.Username,
                Score = entry.Score,
                Rank = entry.Rank,
                RecordedAt = entry.RecordedAt
            })
            .ToListAsync();
    }

    public async Task RecordScoreAsync(int userId, decimal score, string? usernameOverride = null)
    {
        var entry = await _context.LeaderboardEntries
            .FirstOrDefaultAsync(e => e.UserId == userId);

        var username = await _context.Users
            .Where(u => u.UserId == userId)
            .Select(u => u.Username)
            .FirstOrDefaultAsync() ?? "Player";

        if (!string.IsNullOrWhiteSpace(usernameOverride))
        {
            username = usernameOverride;
        }

        if (entry is null)
        {
            entry = new LeaderboardEntry
            {
                UserId = userId,
                Username = username,
                Score = score,
                RecordedAt = DateTime.UtcNow
            };

            _context.LeaderboardEntries.Add(entry);
        }
        else
        {
            entry.Username = username;
            entry.Score = Math.Max(entry.Score, score);
            entry.RecordedAt = DateTime.UtcNow;
        }

        await _context.SaveChangesAsync();

        // Recalculate all ranks
        await RecalculateRanksAsync();
    }

    private async Task RecalculateRanksAsync()
    {
        var entries = await _context.LeaderboardEntries
            .OrderByDescending(e => e.Score)
            .ToListAsync();

        for (int i = 0; i < entries.Count; i++)
        {
            entries[i].Rank = i + 1;
        }

        await _context.SaveChangesAsync();
    }
}
