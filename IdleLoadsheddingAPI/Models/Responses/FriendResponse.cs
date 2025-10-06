namespace IdleLoadsheddingAPI.Models.Responses;

public class FriendResponse
{
    public int UserId { get; set; }
    public string Username { get; set; } = string.Empty;
    public DateTime AddedAt { get; set; }
}
