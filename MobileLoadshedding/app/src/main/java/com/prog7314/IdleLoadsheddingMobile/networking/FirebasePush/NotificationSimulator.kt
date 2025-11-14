package com.prog7314.IdleLoadsheddingMobile.networking.FirebasePush

import android.content.Context
import android.util.Log
import com.google.firebase.messaging.RemoteMessage
import com.prog7314.IdleLoadsheddingMobile.data.models.NotificationType

/**
 * Helper class to simulate push notifications for testing purposes.
 * 
 * This class creates mock RemoteMessage objects that simulate incoming FCM messages,
 * allowing developers to test the notification system without needing to send actual
 * push notifications from Firebase Console or backend.
 * 
 * Usage:
 * ```kotlin
 * NotificationSimulator.simulateEventStarted(context)
 * ```
 */
object NotificationSimulator {
    
    private const val TAG = "NotificationSimulator"
    
    /**
     * Simulates an "Event Started" notification.
     */
    fun simulateEventStarted(context: Context) {
        Log.d(TAG, "Simulating EVENT_STARTED notification")
        
        val data = mapOf(
            "type" to NotificationType.EVENT_STARTED.name,
            "notificationId" to System.currentTimeMillis().toString(),
            "eventId" to "event_123",
            "eventName" to "Weekend Power Challenge"
        )
        
        sendMockNotification(
            context = context,
            title = "⚡ New Event Started!",
            body = "Weekend Power Challenge is now live. Join now to earn bonus rewards!",
            data = data
        )
    }
    
    /**
     * Simulates an "Event Ending Soon" notification.
     */
    fun simulateEventEndingSoon(context: Context) {
        Log.d(TAG, "Simulating EVENT_ENDING_SOON notification")
        
        val data = mapOf(
            "type" to NotificationType.EVENT_ENDING_SOON.name,
            "notificationId" to System.currentTimeMillis().toString(),
            "eventId" to "event_456",
            "eventName" to "Daily Energy Sprint",
            "timeRemaining" to "30 minutes"
        )
        
        sendMockNotification(
            context = context,
            title = "⏰ Event Ending Soon!",
            body = "Daily Energy Sprint ends in 30 minutes. Complete your tasks now!",
            data = data
        )
    }
    
    /**
     * Simulates a "Friend Request" notification.
     */
    fun simulateFriendRequest(context: Context) {
        Log.d(TAG, "Simulating FRIEND_REQUEST notification")
        
        val data = mapOf(
            "type" to NotificationType.FRIEND_REQUEST.name,
            "notificationId" to System.currentTimeMillis().toString(),
            "friendId" to "user_789",
            "friendName" to "PowerPlayer123"
        )
        
        sendMockNotification(
            context = context,
            title = "👋 New Friend Request",
            body = "PowerPlayer123 wants to be your friend!",
            data = data
        )
    }
    
    /**
     * Simulates a "Friend Accepted" notification.
     */
    fun simulateFriendAccepted(context: Context) {
        Log.d(TAG, "Simulating FRIEND_ACCEPTED notification")
        
        val data = mapOf(
            "type" to NotificationType.FRIEND_ACCEPTED.name,
            "notificationId" to System.currentTimeMillis().toString(),
            "friendId" to "user_321",
            "friendName" to "EnergyMaster99"
        )
        
        sendMockNotification(
            context = context,
            title = "✅ Friend Request Accepted",
            body = "EnergyMaster99 accepted your friend request. Start competing now!",
            data = data
        )
    }
    
    /**
     * Simulates a "System Announcement" notification.
     */
    fun simulateSystemAnnouncement(context: Context) {
        Log.d(TAG, "Simulating SYSTEM_ANNOUNCEMENT notification")
        
        val data = mapOf(
            "type" to NotificationType.SYSTEM_ANNOUNCEMENT.name,
            "notificationId" to System.currentTimeMillis().toString(),
            "announcementId" to "announcement_001"
        )
        
        sendMockNotification(
            context = context,
            title = "📢 System Announcement",
            body = "New features added! Check out the improved leaderboard and updated achievements.",
            data = data
        )
    }
    
    /**
     * Simulates a "Maintenance Required" notification.
     */
    fun simulateMaintenanceRequired(context: Context) {
        Log.d(TAG, "Simulating MAINTENANCE_REQUIRED notification")
        
        val data = mapOf(
            "type" to NotificationType.MAINTENANCE_REQUIRED.name,
            "notificationId" to System.currentTimeMillis().toString(),
            "maintenanceStart" to "2025-11-15T02:00:00Z",
            "maintenanceDuration" to "2 hours"
        )
        
        sendMockNotification(
            context = context,
            title = "🔧 Scheduled Maintenance",
            body = "Server maintenance scheduled for tonight at 2:00 AM (2 hours). Please save your progress.",
            data = data
        )
    }
    
    /**
     * Simulates a "General" notification.
     */
    fun simulateGeneral(context: Context) {
        Log.d(TAG, "Simulating GENERAL notification")
        
        val data = mapOf(
            "type" to NotificationType.GENERAL.name,
            "notificationId" to System.currentTimeMillis().toString()
        )
        
        sendMockNotification(
            context = context,
            title = "📬 Idle Loadshedding",
            body = "You've earned offline rewards! Come back to collect them.",
            data = data
        )
    }
    
    /**
     * Sends a mock notification by directly calling the FirebaseMessagingService.
     */
    private fun sendMockNotification(
        context: Context,
        title: String,
        body: String,
        data: Map<String, String>
    ) {
        try {
            // Create a mock RemoteMessage using data-only payload
            val remoteMessageBuilder = RemoteMessage.Builder("mock_sender_id")
                .setData(data.toMutableMap().apply {
                    put("title", title)
                    put("body", body)
                })
            
            val remoteMessage = remoteMessageBuilder.build()
            
            // Create an instance of the messaging service
            val messagingService = IdleLoadsheddingMessagingService()
            
            // Use reflection to attach base context
            try {
                val method = android.app.Service::class.java.getDeclaredMethod(
                    "attachBaseContext",
                    Context::class.java
                )
                method.isAccessible = true
                method.invoke(messagingService, context.applicationContext)
            } catch (e: Exception) {
                Log.w(TAG, "Could not attach base context via reflection", e)
            }
            
            // Call onMessageReceived with the mock message
            messagingService.onMessageReceived(remoteMessage)
            
            Log.d(TAG, "Mock notification sent successfully: $title")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send mock notification", e)
        }
    }
}
