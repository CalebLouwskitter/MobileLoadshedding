
namespace IdleLoadsheddingAPI.Models.DTOs;

public record StatsRequestDto(
    string PlayerId,
    double TotalEnergyGenerated,
    double CurrentEnergy,
    double EnergyPerSecond,
    int ActiveGenerators,
    int GeneratorLevelSum,
    double OfflineSeconds,
    string? LastEventId,
    DateTime TimestampUtc
);

public record StatsSummaryDto(
    int TotalPlayers,
    double AverageEnergyPerSecond,
    IReadOnlyCollection<StatsPerformerDto> TopPerformers
);

public record StatsPerformerDto(
    string PlayerId,
    string DisplayName,
    double Score
);
