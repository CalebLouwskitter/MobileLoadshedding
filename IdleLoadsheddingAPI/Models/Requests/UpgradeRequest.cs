using System.ComponentModel.DataAnnotations;

namespace IdleLoadsheddingAPI.Models.Requests;

public class UpgradeRequest
{
    [Required]
    [MaxLength(100)]
    public string Name { get; set; } = string.Empty;

    [Range(0, double.MaxValue)]
    public decimal Cost { get; set; }

    [Range(0, double.MaxValue)]
    public decimal ElectricityBoost { get; set; }
}
