using System.ComponentModel.DataAnnotations;

namespace IdleLoadsheddingAPI.Models.Requests;

public class StatsSnapshotRequest
{
    [Required]
    public string PlayerId { get; set; } = string.Empty;

    [Range(0, double.MaxValue)]
    public double TotalEnergyGenerated { get; set; }

    [Range(0, double.MaxValue)]
    public double CurrentEnergy { get; set; }

    [Range(0, double.MaxValue)]
    public double EnergyPerSecond { get; set; }

    [Range(0, int.MaxValue)]
    public int ActiveGenerators { get; set; }

    [Range(0, int.MaxValue)]
    public int GeneratorLevelSum { get; set; }

    [Range(0, double.MaxValue)]
    public double OfflineSeconds { get; set; }

    public string? LastEventId { get; set; }

    [Required]
    public string TimestampUtc { get; set; } = string.Empty;

    [Range(0, double.MaxValue)]
    public double CurrentMoney { get; set; }

    [Range(0, double.MaxValue)]
    public double TotalMoneyEarned { get; set; }

    [Range(0, double.MaxValue)]
    public double HoursUntilMaintenance { get; set; }

    [Range(0, double.MaxValue)]
    public double CoreChargePercent { get; set; }

    public List<DistrictSnapshot> Districts { get; set; } = new();

    public class DistrictSnapshot
    {
        [Required]
        public string Id { get; set; } = string.Empty;

        [Required]
        public string Name { get; set; } = string.Empty;

        [Range(0, int.MaxValue)]
        public int Level { get; set; }

        [Range(0, double.MaxValue)]
        public double Efficiency { get; set; }

        [Range(0, double.MaxValue)]
        public double PowerPerSecond { get; set; }

        [Range(0, double.MaxValue)]
        public double MoneyPerSecond { get; set; }

        [Range(0, double.MaxValue)]
        public double HoursUntilMaintenance { get; set; }

        [Range(0, double.MaxValue)]
        public double NextUpgradeCost { get; set; }
    }
}
