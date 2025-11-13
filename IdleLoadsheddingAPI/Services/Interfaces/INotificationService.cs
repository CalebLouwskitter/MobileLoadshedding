using IdleLoadsheddingAPI.Models.Requests;
using IdleLoadsheddingAPI.Models.Responses;

namespace IdleLoadsheddingAPI.Services.Interfaces;

/// <summary>
/// Service interface for managing push notifications and user notification preferences.
/// Handles FCM token registration, notification tracking, and preference management.
/// </summary>
public interface INotificationService
{
    /// <summary>
    /// Registers or updates an FCM token for a user's device.
    /// </summary>
    /// <param name="request">FCM token registration request containing device info</param>
    /// <returns>Registration result response</returns>
    Task<FcmTokenResponse> RegisterFcmTokenAsync(FcmTokenRequest request);

    /// <summary>
    /// Tracks a notification action (opened, dismissed, etc.) for analytics.
    /// </summary>
    /// <param name="firebaseUid">User's Firebase UID</param>
    /// <param name="request">Action tracking request</param>
    /// <returns>Task representing the async operation</returns>
    Task TrackNotificationActionAsync(string firebaseUid, NotificationActionRequest request);

    /// <summary>
    /// Retrieves notification preferences for a user.
    /// </summary>
    /// <param name="firebaseUid">User's Firebase UID</param>
    /// <returns>User's notification preferences</returns>
    Task<NotificationPreferencesResponse> GetNotificationPreferencesAsync(string firebaseUid);

    /// <summary>
    /// Updates notification preferences for a user.
    /// </summary>
    /// <param name="firebaseUid">User's Firebase UID</param>
    /// <param name="request">Updated notification preferences</param>
    /// <returns>Task representing the async operation</returns>
    Task UpdateNotificationPreferencesAsync(string firebaseUid, UpdateNotificationPreferencesRequest request);
}
