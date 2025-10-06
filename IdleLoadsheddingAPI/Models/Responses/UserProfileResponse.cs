namespace IdleLoadsheddingAPI.Models.Responses;

public class UserProfileResponse
{
    public int UserId { get; set; }
    public string FirebaseUid { get; set; } = string.Empty;
    public string Username { get; set; } = string.Empty;
    public string Email { get; set; } = string.Empty;
    public decimal Money { get; set; }
    public decimal Electricity { get; set; }
    public int PrestigeLevel { get; set; }
}
