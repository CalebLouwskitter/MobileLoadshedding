// File intentionally left blank after schema simplification.
using System.ComponentModel.DataAnnotations;

namespace IdleLoadsheddingAPI.Models.Entities;

public class Upgrade
{
    [Key]
    public int UpgradeId { get; set; }

    [MaxLength(100)]
    public string Name { get; set; } = string.Empty;

    [MaxLength(500)]
    public string? Description { get; set; }

    [MaxLength(50)]
    public string UpgradeType { get; set; } = string.Empty;

    public long BaseCost { get; set; }

    public double CostMultiplier { get; set; } = 1.15;

    public double BaseEffect { get; set; }

    public double EffectPerLevel { get; set; }

    public int? MaxLevel { get; set; }

    public DateTime CreatedAtUtc { get; set; } = DateTime.UtcNow;

    [Timestamp]
    public byte[] RowVersion { get; set; } = Array.Empty<byte>();

    public ICollection<PlayerUpgrade> PlayerUpgrades { get; set; } = new List<PlayerUpgrade>();
}
