using System.ComponentModel.DataAnnotations;

namespace IdleLoadsheddingAPI.Models.AppEntities;

public class Friend
{
    [Key]
    public int FriendId { get; set; }

    [Required]
    public int UserId { get; set; }

    [Required]
    public int FriendUserId { get; set; }

    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;

    public User? User { get; set; }

    public User? FriendUser { get; set; }
}
