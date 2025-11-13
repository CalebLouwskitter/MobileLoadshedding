using IdleLoadsheddingAPI.Data;
using IdleLoadsheddingAPI.Models.AppEntities;
using IdleLoadsheddingAPI.Models.Requests;
using IdleLoadsheddingAPI.Models.Responses;
using IdleLoadsheddingAPI.Services.Interfaces;
using Microsoft.EntityFrameworkCore;

namespace IdleLoadsheddingAPI.Services.Implementations;

/// <summary>
/// Implementation of notification service using SQL Server storage.
/// Manages FCM tokens, notification tracking, and user preferences.
/// </summary>
public class NotificationService(GameDbContext context) : INotificationService
{
    private readonly GameDbContext _context = context;

    public async Task<FcmTokenResponse> RegisterFcmTokenAsync(FcmTokenRequest request)
    {
        try
        {
            // Find the user by Firebase UID
            var user = await _context.Users
                .FirstOrDefaultAsync(u => u.FirebaseUid == request.FirebaseUid);

            if (user == null)
            {
                return new FcmTokenResponse
                {
                    Success = false,
                    Message = "User not found"
                };
            }

            // Store or update FCM token
            // In a full implementation, you might want a separate FcmToken table
            // For now, we'll store it as a user property
            user.FcmToken = request.FcmToken;
            user.FcmDeviceId = request.DeviceId;
            user.FcmPlatform = request.Platform;
            user.FcmTokenUpdatedAt = DateTime.UtcNow;

            await _context.SaveChangesAsync();

            return new FcmTokenResponse
            {
                Success = true,
                Message = "FCM token registered successfully"
            };
        }
        catch (Exception ex)
        {
            return new FcmTokenResponse
            {
                Success = false,
                Message = $"Error registering FCM token: {ex.Message}"
            };
        }
    }

    public async Task TrackNotificationActionAsync(string firebaseUid, NotificationActionRequest request)
    {
        // Find the user
        var user = await _context.Users
            .FirstOrDefaultAsync(u => u.FirebaseUid == firebaseUid);

        if (user == null)
        {
            return;
        }

        // In a full implementation, you might want to store notification actions in a separate table
        // For now, we'll just update a last action timestamp
        user.LastNotificationActionAt = DateTime.UtcNow;
        await _context.SaveChangesAsync();
    }

    public async Task<NotificationPreferencesResponse> GetNotificationPreferencesAsync(string firebaseUid)
    {
        var user = await _context.Users
            .FirstOrDefaultAsync(u => u.FirebaseUid == firebaseUid);

        if (user == null)
        {
            // Return default preferences if user not found
            return new NotificationPreferencesResponse
            {
                Preferences = new Models.Responses.NotificationPreferences
                {
                    FirebaseUid = firebaseUid,
                    EnableEvents = true,
                    EnableFriends = true,
                    EnableLeaderboard = true,
                    EnableMaintenance = true,
                    EnableMilestones = true,
                    EnableOfflineEarnings = true,
                    EnableSystemAnnouncements = true
                },
                LastUpdatedUtc = DateTime.UtcNow.ToString("O")
            };
        }

        return new NotificationPreferencesResponse
        {
            Preferences = new Models.Responses.NotificationPreferences
            {
                FirebaseUid = firebaseUid,
                EnableEvents = user.NotifEnableEvents,
                EnableFriends = user.NotifEnableFriends,
                EnableLeaderboard = user.NotifEnableLeaderboard,
                EnableMaintenance = user.NotifEnableMaintenance,
                EnableMilestones = user.NotifEnableMilestones,
                EnableOfflineEarnings = user.NotifEnableOfflineEarnings,
                EnableSystemAnnouncements = user.NotifEnableSystemAnnouncements
            },
            LastUpdatedUtc = (user.NotificationPreferencesUpdatedAt ?? user.CreatedAt).ToString("O")
        };
    }

    public async Task UpdateNotificationPreferencesAsync(string firebaseUid, UpdateNotificationPreferencesRequest request)
    {
        var user = await _context.Users
            .FirstOrDefaultAsync(u => u.FirebaseUid == firebaseUid);

        if (user == null)
        {
            throw new InvalidOperationException("User not found");
        }

        // Update preferences
        user.NotifEnableEvents = request.Preferences.EnableEvents;
        user.NotifEnableFriends = request.Preferences.EnableFriends;
        user.NotifEnableLeaderboard = request.Preferences.EnableLeaderboard;
        user.NotifEnableMaintenance = request.Preferences.EnableMaintenance;
        user.NotifEnableMilestones = request.Preferences.EnableMilestones;
        user.NotifEnableOfflineEarnings = request.Preferences.EnableOfflineEarnings;
        user.NotifEnableSystemAnnouncements = request.Preferences.EnableSystemAnnouncements;
        user.NotificationPreferencesUpdatedAt = DateTime.UtcNow;

        await _context.SaveChangesAsync();
    }
}
