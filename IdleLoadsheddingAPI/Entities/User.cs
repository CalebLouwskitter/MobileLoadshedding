using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace IdleLoadsheddingAPI.Entities;

public class User
{
    [Key]
    public int UserId { get; set; }

    [Required]
    [MaxLength(128)]
    public string FirebaseUid { get; set; } = string.Empty;

    [Required]
    [MaxLength(50)]
    public string Username { get; set; } = string.Empty;

    [Required]
    [MaxLength(100)]
    public string Email { get; set; } = string.Empty;

    [Column(TypeName = "decimal(18,2)")]
    public decimal Money { get; set; }

    [Column(TypeName = "decimal(18,2)")]
    public decimal Electricity { get; set; }

    public int PrestigeLevel { get; set; }

    public DateTime CreatedAt { get; set; }

    public DateTime? LastSyncAt { get; set; }

    public DateTime? LastLoginAt { get; set; }

    public ICollection<Neighbourhood> Neighbourhoods { get; set; } = new List<Neighbourhood>();

    public ICollection<Upgrade> Upgrades { get; set; } = new List<Upgrade>();

    public ICollection<Friend> Friends { get; set; } = new List<Friend>();

    public ICollection<LeaderboardEntry> LeaderboardEntries { get; set; } = new List<LeaderboardEntry>();

    public ICollection<StatsSnapshot> StatsSnapshots { get; set; } = new List<StatsSnapshot>();

    public ICollection<SaveSnapshot> SaveSnapshots { get; set; } = new List<SaveSnapshot>();
}
