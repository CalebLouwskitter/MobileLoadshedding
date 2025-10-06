// File intentionally left blank after simplification.
using IdleLoadsheddingAPI.Data;
using IdleLoadsheddingAPI.Models.DTOs;
using IdleLoadsheddingAPI.Services.Interfaces;
using Microsoft.Data.SqlClient;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Logging;

namespace IdleLoadsheddingAPI.Services.Implementations;

public class LeaderboardService : ILeaderboardService
{
    private readonly ApplicationDbContext _dbContext;
    private readonly ILogger<LeaderboardService> _logger;

    public LeaderboardService(ApplicationDbContext dbContext, ILogger<LeaderboardService> logger)
    {
        _dbContext = dbContext;
        _logger = logger;
    }

    public async Task UpdatePlayerScoreAsync(LeaderboardRequestDto request, CancellationToken cancellationToken = default)
    {
        var playerExists = await _dbContext.Players.AnyAsync(p => p.PlayerId == request.PlayerId, cancellationToken);
        if (!playerExists)
        {
            throw new KeyNotFoundException($"Player {request.PlayerId} not found");
        }

        var playerIdParam = new SqlParameter("@PlayerId", request.PlayerId);
        var displayNameParam = new SqlParameter("@DisplayName", request.DisplayName ?? string.Empty);
        var scoreParam = new SqlParameter("@Score", request.Score);

        await _dbContext.Database.ExecuteSqlRawAsync(
            "EXEC dbo.sp_UpdateLeaderboard @PlayerId, @DisplayName, @Score",
            parameters: new[] { playerIdParam, displayNameParam, scoreParam },
            cancellationToken: cancellationToken);
    }

    public async Task<LeaderboardResponseDto> GetLeaderboardAsync(int take = 50, CancellationToken cancellationToken = default)
    {
        take = Math.Clamp(take, 1, 200);

        var entries = await _dbContext.LeaderboardEntries
            .Include(e => e.Player)
            .OrderByDescending(e => e.Score)
            .ThenBy(e => e.LastUpdatedUtc)
            .Take(take)
            .Select(e => new LeaderboardEntryDto(
                e.PlayerId,
                e.Player.DisplayName,
                (double)e.Score,
                e.LastUpdatedUtc))
            .ToListAsync(cancellationToken);

        return new LeaderboardResponseDto(entries, DateTime.UtcNow);
    }
}
