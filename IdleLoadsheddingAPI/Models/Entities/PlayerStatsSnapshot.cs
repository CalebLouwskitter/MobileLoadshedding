// File intentionally left blank after schema simplification.
using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace IdleLoadsheddingAPI.Models.Entities;

public class PlayerStatsSnapshot
{
    [Key]
    [DatabaseGenerated(DatabaseGeneratedOption.Identity)]
    public long SnapshotId { get; set; }

    [MaxLength(64)]
    public string PlayerId { get; set; } = default!;

    public decimal TotalEnergyGenerated { get; set; }

    public decimal CurrentEnergy { get; set; }

    public decimal EnergyPerSecond { get; set; }

    public int ActiveGenerators { get; set; }

    public int GeneratorLevelSum { get; set; }

    public decimal OfflineSeconds { get; set; }

    [MaxLength(64)]
    public string? LastEventId { get; set; }

    public DateTime TimestampUtc { get; set; } = DateTime.UtcNow;

    public DateTime ReceivedAt { get; set; } = DateTime.UtcNow;

    [Timestamp]
    public byte[] RowVersion { get; set; } = Array.Empty<byte>();

    public Player Player { get; set; } = default!;
}
