namespace IdleLoadsheddingAPI.Models.Responses;

/// <summary>
/// Response from FCM token registration.
/// </summary>
public class FcmTokenResponse
{
    public bool Success { get; set; }
    public string? Message { get; set; }
}

/// <summary>
/// Response containing notification preferences for a user.
/// </summary>
public class NotificationPreferencesResponse
{
    public NotificationPreferences Preferences { get; set; } = new();
    public string LastUpdatedUtc { get; set; } = string.Empty;
}

/// <summary>
/// Notification preferences data.
/// </summary>
public class NotificationPreferences
{
    public string FirebaseUid { get; set; } = string.Empty;
    public bool EnableEvents { get; set; } = true;
    public bool EnableFriends { get; set; } = true;
    public bool EnableLeaderboard { get; set; } = true;
    public bool EnableMaintenance { get; set; } = true;
    public bool EnableMilestones { get; set; } = true;
    public bool EnableOfflineEarnings { get; set; } = true;
    public bool EnableSystemAnnouncements { get; set; } = true;
}
