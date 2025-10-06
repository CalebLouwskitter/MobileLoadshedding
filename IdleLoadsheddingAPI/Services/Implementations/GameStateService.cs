// File intentionally left blank after simplification.
using System.Text.Json;
using IdleLoadsheddingAPI.Data;
using IdleLoadsheddingAPI.Models.DTOs;
using IdleLoadsheddingAPI.Models.Entities;
using IdleLoadsheddingAPI.Services.Interfaces;
using Microsoft.EntityFrameworkCore;

namespace IdleLoadsheddingAPI.Services.Implementations;

public class GameStateService : IGameStateService
{
    private readonly ApplicationDbContext _dbContext;
    private readonly IOfflineProgressService _offlineProgressService;
    private readonly ILogger<GameStateService> _logger;

    public GameStateService(ApplicationDbContext dbContext, IOfflineProgressService offlineProgressService, ILogger<GameStateService> logger)
    {
        _dbContext = dbContext;
        _offlineProgressService = offlineProgressService;
        _logger = logger;
    }

    public async Task<SyncResponseDto> SyncGameStateAsync(string playerId, SyncRequestDto request, CancellationToken cancellationToken = default)
    {
        var player = await _dbContext.Players
            .Include(p => p.SaveState)
            .Include(p => p.Neighbourhoods).ThenInclude(n => n.Neighbourhood)
            .Include(p => p.Upgrades)
            .FirstOrDefaultAsync(p => p.PlayerId == playerId, cancellationToken)
            ?? throw new KeyNotFoundException($"Player {playerId} not found");

        var now = DateTime.UtcNow;
        var lastSync = player.LastSyncAt ?? player.CreatedAt;
        var offlineResult = await _offlineProgressService.CalculateOfflineProgressAsync(playerId, lastSync, now, cancellationToken);

        var baseMoney = (long)Math.Round(request.Currency);
        var moneyAfterOffline = baseMoney + offlineResult.MoneyEarned;
        if (moneyAfterOffline < 0) moneyAfterOffline = 0;

        var baseElectricity = Math.Max(0, request.Electricity);
        var electricityAfterOffline = baseElectricity + offlineResult.ElectricityGained;

        var saveState = player.SaveState ?? new PlayerSaveState
        {
            PlayerId = playerId,
            CreatedAt = now,
            TimestampUtc = now,
            StatsJson = "{}"
        };

        var savePayload = new SaveProjection
        {
            Money = moneyAfterOffline,
            Electricity = electricityAfterOffline,
            MaxElectricity = Math.Max(electricityAfterOffline, baseElectricity),
            ElectricityGenRate = EstimateGenerationRate(request, offlineResult)
        };

        saveState.StatsJson = JsonSerializer.Serialize(savePayload);
        saveState.TimestampUtc = now;
        if (player.SaveState is null)
        {
            _dbContext.PlayerSaveStates.Add(saveState);
            player.SaveState = saveState;
        }

        ApplyNeighbourhoodUpdates(player, request.Neighbourhoods);

        player.LastSyncAt = now;
        player.TotalMoneyEarned = Math.Max(player.TotalMoneyEarned, moneyAfterOffline);
        player.UpdatedAt = now;

        await _dbContext.SaveChangesAsync(cancellationToken);

        var neighbourhoodDtos = player.Neighbourhoods
            .OrderBy(n => n.Neighbourhood.SortOrder)
            .Select(n => new NeighbourhoodDto(
                n.NeighbourhoodId,
                n.Neighbourhood.Name,
                n.IsUnlocked,
                n.IsActive,
                n.Level))
            .ToList();

        return new SyncResponseDto(
            Money: moneyAfterOffline,
            Electricity: electricityAfterOffline,
            MaxElectricity: savePayload.MaxElectricity,
            GenerationRate: savePayload.ElectricityGenRate,
            OfflineProgress: offlineResult,
            Neighbourhoods: neighbourhoodDtos,
            ServerTimestampUtc: now
        );
    }

    public async Task<GameStateDto> GetGameStateAsync(string playerId, CancellationToken cancellationToken = default)
    {
        var player = await _dbContext.Players
            .Include(p => p.SaveState)
            .Include(p => p.Neighbourhoods).ThenInclude(n => n.Neighbourhood)
            .FirstOrDefaultAsync(p => p.PlayerId == playerId, cancellationToken)
            ?? throw new KeyNotFoundException($"Player {playerId} not found");

        var saveProjection = DeserializeSaveProjection(player.SaveState?.StatsJson);

        var neighbourhoods = player.Neighbourhoods
            .OrderBy(n => n.Neighbourhood.SortOrder)
            .Select(n => new NeighbourhoodDto(
                n.NeighbourhoodId,
                n.Neighbourhood.Name,
                n.IsUnlocked,
                n.IsActive,
                n.Level))
            .ToList();

        return new GameStateDto(
            Money: saveProjection.Money,
            Electricity: saveProjection.Electricity,
            MaxElectricity: saveProjection.MaxElectricity,
            GenerationRate: saveProjection.ElectricityGenRate,
            Neighbourhoods: neighbourhoods
        );
    }

    private static double EstimateGenerationRate(SyncRequestDto request, OfflineProgressDto offlineResult)
    {
        if (offlineResult.TimeElapsedSeconds > 0)
        {
            return Math.Max(request.Electricity / Math.Max(1, request.OfflineSeconds), offlineResult.ElectricityGained / offlineResult.TimeElapsedSeconds);
        }

        return request.Electricity / Math.Max(1, request.OfflineSeconds);
    }

    private void ApplyNeighbourhoodUpdates(Player player, IReadOnlyCollection<NeighbourhoodStateDto> updates)
    {
        if (updates.Count == 0)
        {
            return;
        }

        foreach (var update in updates)
        {
            var existing = player.Neighbourhoods.FirstOrDefault(n => n.NeighbourhoodId == update.Id);
            if (existing != null)
            {
                existing.IsActive = update.IsActive;
                existing.IsUnlocked = update.IsUnlocked || existing.IsUnlocked;
                existing.Level = Math.Max(1, update.Level);
            }
            else
            {
                player.Neighbourhoods.Add(new PlayerNeighbourhood
                {
                    PlayerId = player.PlayerId,
                    NeighbourhoodId = update.Id,
                    IsActive = update.IsActive,
                    IsUnlocked = update.IsUnlocked,
                    Level = Math.Max(1, update.Level)
                });
            }
        }
    }

    private static SaveProjection DeserializeSaveProjection(string? json)
    {
        if (string.IsNullOrWhiteSpace(json))
        {
            return new SaveProjection();
        }

        try
        {
            var projection = JsonSerializer.Deserialize<SaveProjection>(json);
            return projection ?? new SaveProjection();
        }
        catch (JsonException)
        {
            return new SaveProjection();
        }
    }

    private sealed class SaveProjection
    {
        public long Money { get; set; }
        public double Electricity { get; set; }
        public double MaxElectricity { get; set; }
        public double ElectricityGenRate { get; set; }
    }
}
