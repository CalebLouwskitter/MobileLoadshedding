// File intentionally left blank after simplification.
namespace IdleLoadsheddingAPI.Models.DTOs;

public record SaveRequestDto(
    string PlayerId,
    string Stats,
    IReadOnlyCollection<GeneratorStateDto> Generators,
    IReadOnlyCollection<DistrictStateDto> Districts,
    CentralStateDto? Central,
    DateTime TimestampUtc
);

public record GeneratorStateDto(string Id, int Level);

public record DistrictStateDto(string Id, int Level, double Efficiency);

public record CentralStateDto(double Charge);
