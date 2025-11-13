using IdleLoadsheddingAPI.Data;
using IdleLoadsheddingAPI.Models.DTOs;
using IdleLoadsheddingAPI.Models.Entities;
using IdleLoadsheddingAPI.Services.Interfaces;
using Microsoft.EntityFrameworkCore;

namespace IdleLoadsheddingAPI.Services.Implementations;

/// <summary>
/// Implementation of event service using SQL Server storage.
/// Manages game event recording and retrieval.
/// </summary>
public class EventService(GameDbContext context) : IEventService
{
    private readonly GameDbContext _context = context;

    public async Task RecordEventAsync(string firebaseUid, EventRequestDto request)
    {
        // Find the user
        var user = await _context.Users
            .FirstOrDefaultAsync(u => u.FirebaseUid == firebaseUid);

        if (user == null)
        {
            throw new InvalidOperationException("User not found");
        }

        // Create event record
        var gameEvent = new GameEvent
        {
            EventId = request.EventId,
            Title = request.Title,
            Description = request.Description,
            IsStarting = request.IsStarting,
            DurationSeconds = request.DurationSeconds,
            ProductionMultiplier = request.ProductionMultiplier,
            EventTimestampUtc = request.TimestampUtc,
            RecordedAt = DateTime.UtcNow
        };

        _context.GameEvents.Add(gameEvent);
        await _context.SaveChangesAsync();
    }

    public async Task<IEnumerable<EventResponseDto>> GetActiveEventsAsync()
    {
        // Get active events from the last 24 hours that are starting
        var cutoffTime = DateTime.UtcNow.AddHours(-24);
        
        var activeEvents = await _context.GameEventDefinitions
            .Where(e => e.IsActive && e.StartTimeUtc <= DateTime.UtcNow && e.EndTimeUtc >= DateTime.UtcNow)
            .ToListAsync();

        return activeEvents.Select(e => new EventResponseDto(
            EventId: e.EventDefinitionId.ToString(),
            Code: e.EventCode,
            Title: e.Title,
            Description: e.Description,
            ProductionMultiplier: e.ProductionMultiplier,
            DurationSeconds: (int)(e.EndTimeUtc - e.StartTimeUtc).TotalSeconds
        ));
    }
}
