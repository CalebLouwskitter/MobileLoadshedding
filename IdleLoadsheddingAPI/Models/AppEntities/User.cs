using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace IdleLoadsheddingAPI.Models.AppEntities;

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

    [MaxLength(10)]
    public string? FriendCode { get; set; }

    [Column(TypeName = "decimal(18,2)")]
    public decimal Money { get; set; }

    [Column(TypeName = "decimal(18,2)")]
    public decimal Electricity { get; set; }

    public int PrestigeLevel { get; set; }

    public DateTime CreatedAt { get; set; }

    public DateTime? LastSyncAt { get; set; }

    public DateTime? LastLoginAt { get; set; }

    // Push notification fields
    [MaxLength(512)]
    public string? FcmToken { get; set; }

    [MaxLength(128)]
    public string? FcmDeviceId { get; set; }

    [MaxLength(32)]
    public string? FcmPlatform { get; set; }

    public DateTime? FcmTokenUpdatedAt { get; set; }

    public DateTime? LastNotificationActionAt { get; set; }

    // Notification preferences (default to true)
    public bool NotifEnableEvents { get; set; } = true;
    public bool NotifEnableFriends { get; set; } = true;
    public bool NotifEnableLeaderboard { get; set; } = true;
    public bool NotifEnableMaintenance { get; set; } = true;
    public bool NotifEnableMilestones { get; set; } = true;
    public bool NotifEnableOfflineEarnings { get; set; } = true;
    public bool NotifEnableSystemAnnouncements { get; set; } = true;
    public DateTime? NotificationPreferencesUpdatedAt { get; set; }

    public ICollection<Neighbourhood> Neighbourhoods { get; set; } = new List<Neighbourhood>();

    public ICollection<Upgrade> Upgrades { get; set; } = new List<Upgrade>();

    public ICollection<Friend> Friends { get; set; } = new List<Friend>();

    public ICollection<LeaderboardEntry> LeaderboardEntries { get; set; } = new List<LeaderboardEntry>();

    public ICollection<StatsSnapshot> StatsSnapshots { get; set; } = new List<StatsSnapshot>();

    public ICollection<SaveSnapshot> SaveSnapshots { get; set; } = new List<SaveSnapshot>();
}
