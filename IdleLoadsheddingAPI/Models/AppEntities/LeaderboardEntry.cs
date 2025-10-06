using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace IdleLoadsheddingAPI.Models.AppEntities;

public class LeaderboardEntry
{
    [Key]
    public int LeaderboardEntryId { get; set; }

    [Required]
    public int UserId { get; set; }

    [Required]
    [MaxLength(50)]
    public string Username { get; set; } = string.Empty;

    [Column(TypeName = "decimal(18,2)")]
    public decimal Score { get; set; }

    public int Rank { get; set; }

    public DateTime RecordedAt { get; set; } = DateTime.UtcNow;

    public User? User { get; set; }
}
