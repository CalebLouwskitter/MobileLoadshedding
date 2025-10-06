// File intentionally left blank after schema simplification.
using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace IdleLoadsheddingAPI.Models.Entities;

public class LeaderboardEntry
{
    [Key]
    [DatabaseGenerated(DatabaseGeneratedOption.Identity)]
    public long EntryId { get; set; }

    [MaxLength(64)]
    public string PlayerId { get; set; } = default!;

    public decimal Score { get; set; }

    public DateTime LastUpdatedUtc { get; set; } = DateTime.UtcNow;

    [Timestamp]
    public byte[] RowVersion { get; set; } = Array.Empty<byte>();

    public Player Player { get; set; } = default!;
}
