
using IdleLoadsheddingAPI.Models.DTOs;

namespace IdleLoadsheddingAPI.Services.Interfaces;

public interface IGameStateService
{
    Task<SyncResponseDto> SyncGameStateAsync(string playerId, SyncRequestDto request, CancellationToken cancellationToken = default);

    Task<GameStateDto> GetGameStateAsync(string playerId, CancellationToken cancellationToken = default);
}
