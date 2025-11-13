package com.prog7314.IdleLoadsheddingMobile.data.models

import com.google.gson.annotations.SerializedName

/**
 * Data models for Firebase Cloud Messaging (FCM) push notifications.
 * Follows the existing application architecture and naming conventions.
 */

/**
 * Request to register or update an FCM token for a player.
 * Sent to backend when FCM token is generated or refreshed.
 */
data class FcmTokenRequest(
    @SerializedName("firebaseUid") val firebaseUid: String,
    @SerializedName("fcmToken") val fcmToken: String,
    @SerializedName("deviceId") val deviceId: String,
    @SerializedName("platform") val platform: String = "android",
    @SerializedName("timestampUtc") val timestampUtc: String
)

/**
 * Response from FCM token registration.
 */
data class FcmTokenResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String? = null
)

/**
 * Notification payload received from FCM.
 * Contains both notification display data and custom data payload.
 */
data class PushNotificationPayload(
    @SerializedName("notificationId") val notificationId: String,
    @SerializedName("type") val type: NotificationType,
    @SerializedName("title") val title: String,
    @SerializedName("body") val body: String,
    @SerializedName("imageUrl") val imageUrl: String? = null,
    @SerializedName("data") val data: Map<String, String>? = null,
    @SerializedName("timestampUtc") val timestampUtc: String
)

/**
 * Types of push notifications supported by the application.
 * Determines how the notification should be handled and displayed.
 */
enum class NotificationType {
    @SerializedName("event_started")
    EVENT_STARTED,
    
    @SerializedName("event_ending_soon")
    EVENT_ENDING_SOON,
    
    @SerializedName("friend_request")
    FRIEND_REQUEST,
    
    @SerializedName("friend_accepted")
    FRIEND_ACCEPTED,
    
    @SerializedName("leaderboard_update")
    LEADERBOARD_UPDATE,
    
    @SerializedName("maintenance_required")
    MAINTENANCE_REQUIRED,
    
    @SerializedName("energy_milestone")
    ENERGY_MILESTONE,
    
    @SerializedName("offline_earnings_ready")
    OFFLINE_EARNINGS_READY,
    
    @SerializedName("system_announcement")
    SYSTEM_ANNOUNCEMENT,
    
    @SerializedName("general")
    GENERAL
}

/**
 * Local notification action taken by user.
 * Sent to backend for analytics and engagement tracking.
 */
data class NotificationActionRequest(
    @SerializedName("notificationId") val notificationId: String,
    @SerializedName("action") val action: NotificationAction,
    @SerializedName("timestampUtc") val timestampUtc: String
)

enum class NotificationAction {
    @SerializedName("opened")
    OPENED,
    
    @SerializedName("dismissed")
    DISMISSED,
    
    @SerializedName("action_clicked")
    ACTION_CLICKED
}

/**
 * Notification preferences for a player.
 * Allows users to customize which notifications they receive.
 */
data class NotificationPreferences(
    @SerializedName("firebaseUid") val firebaseUid: String,
    @SerializedName("enableEvents") val enableEvents: Boolean = true,
    @SerializedName("enableFriends") val enableFriends: Boolean = true,
    @SerializedName("enableLeaderboard") val enableLeaderboard: Boolean = true,
    @SerializedName("enableMaintenance") val enableMaintenance: Boolean = true,
    @SerializedName("enableMilestones") val enableMilestones: Boolean = true,
    @SerializedName("enableOfflineEarnings") val enableOfflineEarnings: Boolean = true,
    @SerializedName("enableSystemAnnouncements") val enableSystemAnnouncements: Boolean = true
)

/**
 * Request to update notification preferences.
 */
data class UpdateNotificationPreferencesRequest(
    @SerializedName("preferences") val preferences: NotificationPreferences
)

/**
 * Response containing current notification preferences.
 */
data class NotificationPreferencesResponse(
    @SerializedName("preferences") val preferences: NotificationPreferences,
    @SerializedName("lastUpdatedUtc") val lastUpdatedUtc: String
)
