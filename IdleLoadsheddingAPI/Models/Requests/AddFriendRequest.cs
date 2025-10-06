using System.ComponentModel.DataAnnotations;

namespace IdleLoadsheddingAPI.Models.Requests;

public class AddFriendRequest
{
    [Required]
    [MaxLength(128)]
    public string FirebaseUid { get; set; } = string.Empty;
}
