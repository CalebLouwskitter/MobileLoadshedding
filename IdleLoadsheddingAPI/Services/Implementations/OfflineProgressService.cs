
using IdleLoadsheddingAPI.Data;
using IdleLoadsheddingAPI.Models.DTOs;
using IdleLoadsheddingAPI.Services.Interfaces;
using Microsoft.Data.SqlClient;
using Microsoft.EntityFrameworkCore;

namespace IdleLoadsheddingAPI.Services.Implementations;

public class OfflineProgressService : IOfflineProgressService
{
    private readonly ApplicationDbContext _dbContext;
    private readonly ILogger<OfflineProgressService> _logger;

    public OfflineProgressService(ApplicationDbContext dbContext, ILogger<OfflineProgressService> logger)
    {
        _dbContext = dbContext;
        _logger = logger;
    }

    public async Task<OfflineProgressDto> CalculateOfflineProgressAsync(string playerId, DateTime lastSyncUtc, DateTime currentUtc, CancellationToken cancellationToken = default)
    {
        var playerIdParam = new SqlParameter("@PlayerId", playerId);
        var lastSyncParam = new SqlParameter("@LastSyncTime", lastSyncUtc);
        var currentParam = new SqlParameter("@CurrentTime", currentUtc);

        try
        {
            var result = await _dbContext.Database.SqlQueryRaw<OfflineProgressResult>(
                    "EXEC dbo.sp_CalculateOfflineProgress @PlayerId, @LastSyncTime, @CurrentTime",
                    playerIdParam, lastSyncParam, currentParam)
                .AsNoTracking()
                .FirstOrDefaultAsync(cancellationToken);

            if (result == null)
            {
                return new OfflineProgressDto(0, 0, 0);
            }

            return new OfflineProgressDto(result.ElectricityGained, result.MoneyEarned, result.ElapsedSeconds);
        }
        catch (Exception ex)
        {
            _logger.LogWarning(ex, "Failed to calculate offline progress for {PlayerId}", playerId);
            return new OfflineProgressDto(0, 0, 0);
        }
    }

    private sealed record OfflineProgressResult(double ElectricityGained, long MoneyEarned, int ElapsedSeconds);
}
