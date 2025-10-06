// File intentionally left blank after schema simplification.
using System.ComponentModel.DataAnnotations;

namespace IdleLoadsheddingAPI.Models.Entities;

public class GameEventDefinition
{
    [Key]
    public int EventDefinitionId { get; set; }

    [MaxLength(50)]
    public string Code { get; set; } = string.Empty;

    [MaxLength(200)]
    public string Title { get; set; } = string.Empty;

    [MaxLength(500)]
    public string? Description { get; set; }

    public string? EffectJson { get; set; }

    public int TriggerWeight { get; set; }

    public int DurationSeconds { get; set; }

    public DateTime CreatedAtUtc { get; set; } = DateTime.UtcNow;

    [Timestamp]
    public byte[] RowVersion { get; set; } = Array.Empty<byte>();
}
