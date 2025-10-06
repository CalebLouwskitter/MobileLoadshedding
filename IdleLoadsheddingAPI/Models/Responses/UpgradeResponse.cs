namespace IdleLoadsheddingAPI.Models.Responses;

public class UpgradeResponse
{
    public bool Success { get; set; }
    public decimal NewElectricityRate { get; set; }
    public decimal RemainingMoney { get; set; }
}
