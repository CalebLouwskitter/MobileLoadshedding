// File intentionally left blank after schema simplification.
using System.ComponentModel.DataAnnotations;

namespace IdleLoadsheddingAPI.Models.Entities;

public class PlayerUpgrade
{
    [Key]
    public Guid PlayerUpgradeId { get; set; } = Guid.NewGuid();

    [MaxLength(64)]
    public string PlayerId { get; set; } = default!;

    public int UpgradeId { get; set; }

    public int CurrentLevel { get; set; }

    [Timestamp]
    public byte[] RowVersion { get; set; } = Array.Empty<byte>();

    public Player Player { get; set; } = default!;

    public Upgrade Upgrade { get; set; } = default!;
}
