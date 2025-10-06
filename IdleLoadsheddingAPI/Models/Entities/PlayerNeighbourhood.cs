// File intentionally left blank after schema simplification.
using System.ComponentModel.DataAnnotations;

namespace IdleLoadsheddingAPI.Models.Entities;

public class PlayerNeighbourhood
{
    [Key]
    public Guid PlayerNeighbourhoodId { get; set; } = Guid.NewGuid();

    [MaxLength(64)]
    public string PlayerId { get; set; } = default!;

    public int NeighbourhoodId { get; set; }

    public bool IsUnlocked { get; set; }

    public bool IsActive { get; set; }

    public int Level { get; set; } = 1;

    [Timestamp]
    public byte[] RowVersion { get; set; } = Array.Empty<byte>();

    public Player Player { get; set; } = default!;

    public Neighbourhood Neighbourhood { get; set; } = default!;
}
