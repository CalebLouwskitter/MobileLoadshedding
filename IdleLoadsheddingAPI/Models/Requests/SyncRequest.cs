using System.ComponentModel.DataAnnotations;

namespace IdleLoadsheddingAPI.Models.Requests;

public class SyncRequest
{
    [Range(0, double.MaxValue)]
    public decimal MoneyEarned { get; set; }

    [Range(-1000000, 1000000)]
    public decimal ElectricityGenerated { get; set; }

    [Range(0, 999)]
    public int PrestigeLevel { get; set; }
}
