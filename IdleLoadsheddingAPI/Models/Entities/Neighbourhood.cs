// File intentionally left blank after schema simplification.
using System.ComponentModel.DataAnnotations;

namespace IdleLoadsheddingAPI.Models.Entities;

public class Neighbourhood
{
    [Key]
    public int NeighbourhoodId { get; set; }

    [MaxLength(100)]
    public string Name { get; set; } = string.Empty;

    [MaxLength(500)]
    public string? Description { get; set; }

    public double BaseIncome { get; set; }

    public double BasePowerConsumption { get; set; }

    public long UnlockCost { get; set; }

    public int RequiredPrestigeLevel { get; set; }

    public int SortOrder { get; set; }

    public DateTime CreatedAtUtc { get; set; } = DateTime.UtcNow;

    [Timestamp]
    public byte[] RowVersion { get; set; } = Array.Empty<byte>();

    public ICollection<PlayerNeighbourhood> PlayerNeighbourhoods { get; set; } = new List<PlayerNeighbourhood>();
}
