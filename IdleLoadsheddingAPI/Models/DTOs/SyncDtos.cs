// File intentionally left blank after simplification.
namespace IdleLoadsheddingAPI.Models.DTOs;

public record SyncRequestDto(
    string UserId,
    DateTime ClientTimestampUtc,
    double OfflineSeconds,
    double Currency,
    double Electricity,
    IReadOnlyCollection<NeighbourhoodStateDto> Neighbourhoods,
    IReadOnlyCollection<UpgradeStateDto> Upgrades,
    IReadOnlyCollection<string> DirtyFlags
);

public record SyncResponseDto(
    long Money,
    double Electricity,
    double MaxElectricity,
    double GenerationRate,
    OfflineProgressDto OfflineProgress,
    IReadOnlyCollection<NeighbourhoodDto> Neighbourhoods,
    DateTime ServerTimestampUtc
);

public record OfflineProgressDto(
    double ElectricityGained,
    long MoneyEarned,
    int TimeElapsedSeconds
);

public record NeighbourhoodStateDto(
    int Id,
    bool IsActive,
    bool IsUnlocked,
    int Level
);

public record UpgradeStateDto(
    int Id,
    int Level
);

public record NeighbourhoodDto(
    int Id,
    string Name,
    bool IsUnlocked,
    bool IsActive,
    int Level
);

public record GameStateDto(
    long Money,
    double Electricity,
    double MaxElectricity,
    double GenerationRate,
    IReadOnlyCollection<NeighbourhoodDto> Neighbourhoods
);
