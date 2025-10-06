namespace IdleLoadsheddingAPI.Models.Responses;

public class SyncResponse
{
    public decimal TotalMoney { get; set; }
    public decimal OfflineEarnings { get; set; }
    public decimal ElectricityRate { get; set; }
    public int PrestigeLevel { get; set; }
}
