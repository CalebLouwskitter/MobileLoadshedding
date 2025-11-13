using System.ComponentModel.DataAnnotations;

namespace IdleLoadsheddingAPI.Models.Requests;

/// <summary>
/// Request to register or update an FCM token for a player.
/// Sent when the mobile app generates or refreshes its FCM token.
/// </summary>
public class FcmTokenRequest
{
    [Required]
    [MaxLength(128)]
    public string FirebaseUid { get; set; } = string.Empty;

    [Required]
    [MaxLength(512)]
    public string FcmToken { get; set; } = string.Empty;

    [Required]
    [MaxLength(128)]
    public string DeviceId { get; set; } = string.Empty;

    [Required]
    [MaxLength(32)]
    public string Platform { get; set; } = "android";

    [Required]
    public string TimestampUtc { get; set; } = string.Empty;
}

/// <summary>
/// Request to track notification action (opened, dismissed, etc.) for analytics.
/// </summary>
public class NotificationActionRequest
{
    [Required]
    [MaxLength(128)]
    public string NotificationId { get; set; } = string.Empty;

    [Required]
    [MaxLength(32)]
    public string Action { get; set; } = string.Empty;

    [Required]
    public string TimestampUtc { get; set; } = string.Empty;
}

/// <summary>
/// Request to update notification preferences for a user.
/// </summary>
public class UpdateNotificationPreferencesRequest
{
    [Required]
    public NotificationPreferences Preferences { get; set; } = new();
}

/// <summary>
/// Notification preferences for a user.
/// Controls which types of notifications the user wants to receive.
/// </summary>
public class NotificationPreferences
{
    [Required]
    [MaxLength(128)]
    public string FirebaseUid { get; set; } = string.Empty;

    public bool EnableEvents { get; set; } = true;
    public bool EnableFriends { get; set; } = true;
    public bool EnableLeaderboard { get; set; } = true;
    public bool EnableMaintenance { get; set; } = true;
    public bool EnableMilestones { get; set; } = true;
    public bool EnableOfflineEarnings { get; set; } = true;
    public bool EnableSystemAnnouncements { get; set; } = true;
}
