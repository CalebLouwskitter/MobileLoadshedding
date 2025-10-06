
using IdleLoadsheddingAPI.Models.DTOs;

namespace IdleLoadsheddingAPI.Services.Interfaces;

public interface IOfflineProgressService
{
    Task<OfflineProgressDto> CalculateOfflineProgressAsync(string playerId, DateTime lastSyncUtc, DateTime currentUtc, CancellationToken cancellationToken = default);
}
