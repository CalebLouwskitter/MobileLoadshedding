using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace IdleLoadsheddingAPI.Models.AppEntities;

public class Upgrade
{
    [Key]
    public int UpgradeId { get; set; }

    [Required]
    public int UserId { get; set; }

    [Required]
    [MaxLength(100)]
    public string Name { get; set; } = string.Empty;

    [Column(TypeName = "decimal(18,2)")]
    public decimal Cost { get; set; }

    [Column(TypeName = "decimal(18,2)")]
    public decimal ElectricityBoost { get; set; }

    public DateTime PurchasedAt { get; set; } = DateTime.UtcNow;

    public User? User { get; set; }
}
