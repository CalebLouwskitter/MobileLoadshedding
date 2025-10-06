// File intentionally left blank after schema simplification.
using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace IdleLoadsheddingAPI.Models.Entities;

public class BackendStatsRollup
{
    [Key]
    [DatabaseGenerated(DatabaseGeneratedOption.Identity)]
    public long RollupId { get; set; }

    public DateTime PeriodStartUtc { get; set; }

    public DateTime PeriodEndUtc { get; set; }

    public int TotalPlayers { get; set; }

    public decimal AverageEnergyPerSecond { get; set; }

    public DateTime GeneratedAt { get; set; } = DateTime.UtcNow;

    [Timestamp]
    public byte[] RowVersion { get; set; } = Array.Empty<byte>();
}
