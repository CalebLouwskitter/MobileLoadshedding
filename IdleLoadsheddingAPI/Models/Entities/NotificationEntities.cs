using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace IdleLoadsheddingAPI.Models.Entities;

/// <summary>
/// Stores FCM (Firebase Cloud Messaging) tokens for push notifications.
/// Each user can have multiple tokens (one per device).
/// </summary>
public class FcmToken
{
    [Key]
    [DatabaseGenerated(DatabaseGeneratedOption.Identity)]
    public int Id { get; set; }

    [Required]
    [MaxLength(128)]
    public string FirebaseUid { get; set; } = string.Empty;

    [Required]
    [MaxLength(512)]
    public string Token { get; set; } = string.Empty;

    [Required]
    [MaxLength(128)]
    public string DeviceId { get; set; } = string.Empty;

    [MaxLength(32)]
    public string Platform { get; set; } = "android";

    [Required]
    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;

    [Required]
    public DateTime UpdatedAt { get; set; } = DateTime.UtcNow;

    public DateTime? LastUsedAt { get; set; }

    public bool IsActive { get; set; } = true;

    [Timestamp]
    public byte[] RowVersion { get; set; } = Array.Empty<byte>();
}

/// <summary>
/// Stores notification preferences for each user.
/// Controls which types of notifications the user wants to receive.
/// </summary>
public class NotificationPreference
{
    [Key]
    [MaxLength(128)]
    public string FirebaseUid { get; set; } = string.Empty;

    public bool EnableEvents { get; set; } = true;
    public bool EnableFriends { get; set; } = true;
    public bool EnableLeaderboard { get; set; } = true;
    public bool EnableMaintenance { get; set; } = true;
    public bool EnableMilestones { get; set; } = true;
    public bool EnableOfflineEarnings { get; set; } = true;
    public bool EnableSystemAnnouncements { get; set; } = true;

    [Required]
    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;

    [Required]
    public DateTime UpdatedAt { get; set; } = DateTime.UtcNow;

    [Timestamp]
    public byte[] RowVersion { get; set; } = Array.Empty<byte>();
}

/// <summary>
/// Tracks notification actions for analytics.
/// Records when users open, dismiss, or interact with notifications.
/// </summary>
public class NotificationAction
{
    [Key]
    [DatabaseGenerated(DatabaseGeneratedOption.Identity)]
    public int Id { get; set; }

    [Required]
    [MaxLength(128)]
    public string NotificationId { get; set; } = string.Empty;

    [Required]
    [MaxLength(128)]
    public string FirebaseUid { get; set; } = string.Empty;

    [Required]
    [MaxLength(32)]
    public string Action { get; set; } = string.Empty;

    [Required]
    public DateTime ActionTimestamp { get; set; } = DateTime.UtcNow;

    [MaxLength(64)]
    public string? NotificationType { get; set; }

    [Timestamp]
    public byte[] RowVersion { get; set; } = Array.Empty<byte>();
}
