// File intentionally left blank after simplification.
using IdleLoadsheddingAPI.Models.DTOs;

namespace IdleLoadsheddingAPI.Services.Interfaces;

public interface ILeaderboardService
{
    Task UpdatePlayerScoreAsync(LeaderboardRequestDto request, CancellationToken cancellationToken = default);

    Task<LeaderboardResponseDto> GetLeaderboardAsync(int take = 50, CancellationToken cancellationToken = default);
}
