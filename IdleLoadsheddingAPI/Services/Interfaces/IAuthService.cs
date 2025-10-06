using IdleLoadsheddingAPI.Models.Entities;

namespace IdleLoadsheddingAPI.Services.Interfaces;

public interface IAuthService
{
    Task<Player> EnsurePlayerAsync(string firebaseUid, string? email, string? displayName, CancellationToken cancellationToken = default);
}
