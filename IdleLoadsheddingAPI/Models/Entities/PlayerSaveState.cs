// File intentionally left blank after schema simplification.
using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace IdleLoadsheddingAPI.Models.Entities;

public class PlayerSaveState
{
    [Key]
    [DatabaseGenerated(DatabaseGeneratedOption.Identity)]
    public long SaveId { get; set; }

    [MaxLength(64)]
    public string PlayerId { get; set; } = default!;

    [Column(TypeName = "nvarchar(max)")]
    public string StatsJson { get; set; } = "{}";

    public DateTime TimestampUtc { get; set; } = DateTime.UtcNow;

    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;

    [Timestamp]
    public byte[] RowVersion { get; set; } = Array.Empty<byte>();

    public Player Player { get; set; } = default!;

    public ICollection<PlayerSaveGenerator> Generators { get; set; } = new List<PlayerSaveGenerator>();
}
