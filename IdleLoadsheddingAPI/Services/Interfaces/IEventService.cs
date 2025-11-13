using IdleLoadsheddingAPI.Models.DTOs;

namespace IdleLoadsheddingAPI.Services.Interfaces;

/// <summary>
/// Service interface for managing game events.
/// Handles event recording and retrieval of active events.
/// </summary>
public interface IEventService
{
    /// <summary>
    /// Records a game event from Unity.
    /// </summary>
    /// <param name="firebaseUid">User's Firebase UID</param>
    /// <param name="request">Event data from client</param>
    /// <returns>Task representing the async operation</returns>
    Task RecordEventAsync(string firebaseUid, EventRequestDto request);

    /// <summary>
    /// Retrieves active/available game events.
    /// </summary>
    /// <returns>List of active events</returns>
    Task<IEnumerable<EventResponseDto>> GetActiveEventsAsync();
}
