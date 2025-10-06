// File intentionally left blank after simplification.
namespace IdleLoadsheddingAPI.Models.DTOs;

public record EventRequestDto(
    string EventId,
    string Title,
    string? Description,
    bool IsStarting,
    double DurationSeconds,
    double ProductionMultiplier,
    DateTime TimestampUtc
);

public record EventResponseDto(
    string EventId,
    string Code,
    string Title,
    string? Description,
    double ProductionMultiplier,
    int DurationSeconds
);
