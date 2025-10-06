
using System.ComponentModel.DataAnnotations;

namespace IdleLoadsheddingAPI.Models.Entities;

public class GameEvent
{
    [Key]
    [MaxLength(64)]
    public string EventId { get; set; } = Guid.NewGuid().ToString();

    [MaxLength(200)]
    public string Title { get; set; } = string.Empty;

    [MaxLength(1000)]
    public string? Description { get; set; }

    public bool IsStarting { get; set; }

    public double DurationSeconds { get; set; }

    public double ProductionMultiplier { get; set; }

    public DateTime EventTimestampUtc { get; set; } = DateTime.UtcNow;

    public DateTime RecordedAt { get; set; } = DateTime.UtcNow;

    [Timestamp]
    public byte[] RowVersion { get; set; } = Array.Empty<byte>();
}
