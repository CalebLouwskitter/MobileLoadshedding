package com.prog7314.IdleLoadsheddingMobile.networking.FirebasePush

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson
import com.prog7314.IdleLoadsheddingMobile.MainActivity
import com.prog7314.IdleLoadsheddingMobile.R
import com.prog7314.IdleLoadsheddingMobile.data.models.NotificationType
import com.prog7314.IdleLoadsheddingMobile.data.models.PushNotificationPayload

/**
 * Firebase Cloud Messaging service that handles incoming push notifications.
 * 
 * This service:
 * - Receives push notifications from FCM
 * - Processes notification data
 * - Creates and displays notifications to the user
 * - Handles notification clicks and actions
 * - Manages FCM token refresh events
 * 
 * References:
 * - Firebase Cloud Messaging, 2025. Firebase Cloud Messaging Documentation.
 *   https://firebase.google.com/docs/cloud-messaging/android/client
 */
class IdleLoadsheddingMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "FCMService"
        private const val CHANNEL_ID_DEFAULT = "idle_loadshedding_default"
        private const val CHANNEL_ID_EVENTS = "idle_loadshedding_events"
        private const val CHANNEL_ID_FRIENDS = "idle_loadshedding_friends"
        private const val CHANNEL_ID_SYSTEM = "idle_loadshedding_system"
        
        private val gson = Gson()
        
        // Intent extras for notification data
        const val EXTRA_NOTIFICATION_TYPE = "notification_type"
        const val EXTRA_NOTIFICATION_DATA = "notification_data"
        const val EXTRA_NOTIFICATION_ID = "notification_id"
    }

    /**
     * Called when a new FCM token is generated.
     * This happens on initial install and periodically thereafter.
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM token generated: ${token.take(20)}...")
        
        // Register the new token with the backend
        FcmTokenManager.getInstance(applicationContext).refreshToken(token)
    }

    /**
     * Called when a message is received from FCM.
     * Processes the message and displays a notification if appropriate.
     */
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        
        Log.d(TAG, "Message received from: ${message.from}")
        
        // Check if message contains a notification payload
        message.notification?.let { notification ->
            Log.d(TAG, "Notification title: ${notification.title}")
            Log.d(TAG, "Notification body: ${notification.body}")
        }
        
        // Process data payload
        if (message.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: ${message.data}")
            handleDataMessage(message.data)
        }
        
        // If notification payload exists, display it
        message.notification?.let { notification ->
            val notificationType = message.data["type"]?.let {
                try {
                    NotificationType.valueOf(it.uppercase())
                } catch (e: IllegalArgumentException) {
                    NotificationType.GENERAL
                }
            } ?: NotificationType.GENERAL
            
            showNotification(
                title = notification.title ?: "Idle Loadshedding",
                body = notification.body ?: "",
                type = notificationType,
                data = message.data,
                imageUrl = notification.imageUrl?.toString()
            )
        }
    }

    /**
     * Handles custom data messages from FCM.
     * These can be used for background updates without showing a notification.
     */
    private fun handleDataMessage(data: Map<String, String>) {
        val type = data["type"]
        val payload = data["payload"]
        
        when (type) {
            "silent_update" -> {
                // Handle silent background update
                Log.d(TAG, "Processing silent update")
                // Could trigger background sync here
            }
            "data_refresh" -> {
                // Trigger data refresh
                Log.d(TAG, "Triggering data refresh")
                // Could update local cache or trigger API call
            }
            else -> {
                Log.d(TAG, "Unknown data message type: $type")
            }
        }
    }

    /**
     * Creates and displays a notification to the user.
     */
    private fun showNotification(
        title: String,
        body: String,
        type: NotificationType,
        data: Map<String, String>,
        imageUrl: String? = null
    ) {
        // Create notification channel (required for Android O+)
        val channelId = getChannelIdForType(type)
        createNotificationChannel(channelId, getChannelNameForType(type))
        
        // Create intent that opens the app when notification is tapped
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(EXTRA_NOTIFICATION_TYPE, type.name)
            putExtra(EXTRA_NOTIFICATION_DATA, gson.toJson(data))
            putExtra(EXTRA_NOTIFICATION_ID, data["notificationId"] ?: System.currentTimeMillis().toString())
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Get default notification sound
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        
        // Build the notification
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(getIconForType(type))
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)
            .setPriority(getPriorityForType(type))
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
        
        // Add action buttons based on notification type
        addNotificationActions(notificationBuilder, type, data)
        
        // Show the notification
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = data["notificationId"]?.hashCode() ?: System.currentTimeMillis().toInt()
        notificationManager.notify(notificationId, notificationBuilder.build())
        
        Log.d(TAG, "Notification displayed: $title")
    }

    /**
     * Creates a notification channel for Android O and above.
     */
    private fun createNotificationChannel(channelId: String, channelName: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = when (channelId) {
                CHANNEL_ID_EVENTS -> NotificationManager.IMPORTANCE_HIGH
                CHANNEL_ID_FRIENDS -> NotificationManager.IMPORTANCE_DEFAULT
                CHANNEL_ID_SYSTEM -> NotificationManager.IMPORTANCE_HIGH
                else -> NotificationManager.IMPORTANCE_DEFAULT
            }
            
            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = "Notifications for $channelName"
                enableLights(true)
                enableVibration(true)
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Determines the notification channel based on notification type.
     */
    private fun getChannelIdForType(type: NotificationType): String {
        return when (type) {
            NotificationType.EVENT_STARTED,
            NotificationType.EVENT_ENDING_SOON -> CHANNEL_ID_EVENTS
            
            NotificationType.FRIEND_REQUEST,
            NotificationType.FRIEND_ACCEPTED -> CHANNEL_ID_FRIENDS
            
            NotificationType.SYSTEM_ANNOUNCEMENT,
            NotificationType.MAINTENANCE_REQUIRED -> CHANNEL_ID_SYSTEM
            
            else -> CHANNEL_ID_DEFAULT
        }
    }

    /**
     * Gets human-readable channel name for notification type.
     */
    private fun getChannelNameForType(type: NotificationType): String {
        return when (type) {
            NotificationType.EVENT_STARTED,
            NotificationType.EVENT_ENDING_SOON -> "Events"
            
            NotificationType.FRIEND_REQUEST,
            NotificationType.FRIEND_ACCEPTED -> "Friends"
            
            NotificationType.SYSTEM_ANNOUNCEMENT,
            NotificationType.MAINTENANCE_REQUIRED -> "System"
            
            else -> "General"
        }
    }

    /**
     * Gets appropriate icon for notification type.
     */
    private fun getIconForType(type: NotificationType): Int {
        return when (type) {
            NotificationType.EVENT_STARTED,
            NotificationType.EVENT_ENDING_SOON -> R.drawable.ic_launcher_foreground
            
            NotificationType.FRIEND_REQUEST,
            NotificationType.FRIEND_ACCEPTED -> R.drawable.ic_launcher_foreground
            
            else -> R.drawable.ic_launcher_foreground
        }
    }

    /**
     * Gets notification priority based on type.
     */
    private fun getPriorityForType(type: NotificationType): Int {
        return when (type) {
            NotificationType.EVENT_STARTED,
            NotificationType.MAINTENANCE_REQUIRED,
            NotificationType.SYSTEM_ANNOUNCEMENT -> NotificationCompat.PRIORITY_HIGH
            
            NotificationType.FRIEND_REQUEST -> NotificationCompat.PRIORITY_DEFAULT
            
            else -> NotificationCompat.PRIORITY_LOW
        }
    }

    /**
     * Adds action buttons to notification based on type.
     */
    private fun addNotificationActions(
        builder: NotificationCompat.Builder,
        type: NotificationType,
        data: Map<String, String>
    ) {
        when (type) {
            NotificationType.EVENT_STARTED -> {
                // Add "Play Now" action
                val playIntent = Intent(this, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    putExtra(EXTRA_NOTIFICATION_TYPE, type.name)
                    putExtra("action", "play_now")
                }
                val playPendingIntent = PendingIntent.getActivity(
                    this,
                    1,
                    playIntent,
                    PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
                )
                builder.addAction(R.drawable.ic_launcher_foreground, "Play Now", playPendingIntent)
            }
            
            NotificationType.FRIEND_REQUEST -> {
                // Add "View" action
                val viewIntent = Intent(this, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    putExtra(EXTRA_NOTIFICATION_TYPE, type.name)
                    putExtra("action", "view_friends")
                }
                val viewPendingIntent = PendingIntent.getActivity(
                    this,
                    2,
                    viewIntent,
                    PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
                )
                builder.addAction(R.drawable.ic_launcher_foreground, "View", viewPendingIntent)
            }
            
            else -> {
                // No additional actions
            }
        }
    }

    /**
     * Called when a message couldn't be delivered.
     */
    override fun onDeletedMessages() {
        super.onDeletedMessages()
        Log.w(TAG, "Some messages were deleted")
        // Could trigger a full sync here
    }

    /**
     * Called when FCM encounters a send error.
     */
    override fun onMessageSent(msgId: String) {
        super.onMessageSent(msgId)
        Log.d(TAG, "Message sent successfully: $msgId")
    }

    /**
     * Called when there's an error sending a message.
     */
    override fun onSendError(msgId: String, exception: Exception) {
        super.onSendError(msgId, exception)
        Log.e(TAG, "Error sending message: $msgId", exception)
    }
}
