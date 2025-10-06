// File intentionally left blank after simplification.
namespace IdleLoadsheddingAPI.Models.DTOs;

public record LeaderboardRequestDto(
    string PlayerId,
    string DisplayName,
    double Score
);

public record LeaderboardEntryDto(
    string PlayerId,
    string DisplayName,
    double Score,
    DateTime LastUpdatedUtc
);

public record LeaderboardResponseDto(
    IReadOnlyCollection<LeaderboardEntryDto> Entries,
    DateTime TimestampUtc
);
