// File intentionally left blank after schema simplification.
using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace IdleLoadsheddingAPI.Models.Entities;

public class PlayerSaveGenerator
{
    [Key]
    [Column(Order = 0)]
    public long SaveId { get; set; }

    [Key]
    [Column(Order = 1)]
    [MaxLength(64)]
    public string GeneratorId { get; set; } = default!;

    public int Level { get; set; }

    [Timestamp]
    public byte[] RowVersion { get; set; } = Array.Empty<byte>();

    public PlayerSaveState SaveState { get; set; } = default!;
}
