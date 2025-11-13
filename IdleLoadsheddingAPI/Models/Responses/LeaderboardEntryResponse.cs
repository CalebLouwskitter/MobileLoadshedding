namespace IdleLoadsheddingAPI.Models.Responses;

public class LeaderboardEntryResponse
{
    public int PlayerId { get; set; }
    public string Username { get; set; } = string.Empty;
    public decimal Score { get; set; }
    public int Rank { get; set; }
    public DateTime RecordedAt { get; set; }
}
