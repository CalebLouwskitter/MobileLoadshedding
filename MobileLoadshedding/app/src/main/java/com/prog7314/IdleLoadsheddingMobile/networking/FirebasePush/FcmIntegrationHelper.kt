package com.prog7314.IdleLoadsheddingMobile.networking.FirebasePush

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Helper class to simplify FCM integration into activities.
 * Provides convenient methods for initializing FCM and handling notifications.
 * 
 * Usage in MainActivity:
 * ```kotlin
 * class MainActivity : AppCompatActivity() {
 *     override fun onCreate(savedInstanceState: Bundle?) {
 *         super.onCreate(savedInstanceState)
 *         
 *         // Initialize FCM after user authentication
 *         FcmIntegrationHelper.initialize(this)
 *         
 *         // Handle notification click
 *         FcmIntegrationHelper.handleNotificationIntent(intent) { type, data ->
 *             // Navigate based on notification type
 *             when (type) {
 *                 "EVENT_STARTED" -> navigateToEvents()
 *                 "FRIEND_REQUEST" -> navigateToFriends()
 *             }
 *         }
 *     }
 *     
 *     override fun onDestroy() {
 *         super.onDestroy()
 *         // Clean up on logout
 *         if (isLoggingOut) {
 *             FcmIntegrationHelper.cleanup(this)
 *         }
 *     }
 * }
 * ```
 */
object FcmIntegrationHelper {
    
    private const val TAG = "FcmIntegration"
    private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001
    
    /**
     * Initializes FCM for the current authenticated user.
     * Requests notification permissions (Android 13+) and registers FCM token.
     * 
     * @param activity The activity context
     * @param onPermissionGranted Optional callback when permission is granted
     */
    fun initialize(
        activity: Activity,
        onPermissionGranted: (() -> Unit)? = null
    ) {
        // Check if user is authenticated
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Log.w(TAG, "User not authenticated, skipping FCM initialization")
            return
        }
        
        // Request notification permission for Android 13+ (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermission(activity, onPermissionGranted)
        } else {
            // Permission not required for older versions
            initializeFcmToken(activity)
            onPermissionGranted?.invoke()
        }
    }
    
    /**
     * Requests POST_NOTIFICATIONS permission (Android 13+).
     */
    private fun requestNotificationPermission(
        activity: Activity,
        onGranted: (() -> Unit)? = null
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                NOTIFICATION_PERMISSION_REQUEST_CODE
            )
            
            // Note: Callback should be handled in activity's onRequestPermissionsResult
            // For now, just initialize FCM anyway
            initializeFcmToken(activity)
        }
    }
    
    /**
     * Initializes FCM token manager and retrieves token.
     */
    private fun initializeFcmToken(activity: Activity) {
        val fcmManager = FcmTokenManager.getInstance(activity)
        fcmManager.initializeFcm()
        
        // Subscribe to default topics
        fcmManager.subscribeToTopic("all_users")
        
        Log.d(TAG, "FCM initialization completed")
    }
    
    /**
     * Handles intent data from notification clicks.
     * 
     * @param intent The intent that launched the activity
     * @param onNotificationData Callback with notification type and data
     * @return true if intent contained notification data, false otherwise
     */
    fun handleNotificationIntent(
        intent: Intent?,
        onNotificationData: (type: String, data: String) -> Unit
    ): Boolean {
        if (intent == null) return false
        
        val notificationType = intent.getStringExtra(
            IdleLoadsheddingMessagingService.EXTRA_NOTIFICATION_TYPE
        )
        
        val notificationData = intent.getStringExtra(
            IdleLoadsheddingMessagingService.EXTRA_NOTIFICATION_DATA
        )
        
        if (!notificationType.isNullOrBlank() && !notificationData.isNullOrBlank()) {
            Log.d(TAG, "Handling notification: type=$notificationType")
            onNotificationData(notificationType, notificationData)
            return true
        }
        
        return false
    }
    
    /**
     * Cleans up FCM data and unsubscribes from notifications.
     * Should be called when user logs out.
     * 
     * @param activity The activity context
     */
    fun cleanup(activity: Activity) {
        val fcmManager = FcmTokenManager.getInstance(activity)
        fcmManager.unsubscribeFromNotifications()
        
        Log.d(TAG, "FCM cleanup completed")
    }
    
    /**
     * Subscribes to a notification topic.
     * 
     * @param activity The activity context
     * @param topic The topic name (e.g., "events", "leaderboard_updates")
     */
    fun subscribeToTopic(activity: Activity, topic: String) {
        val fcmManager = FcmTokenManager.getInstance(activity)
        fcmManager.subscribeToTopic(topic)
        
        Log.d(TAG, "Subscribed to topic: $topic")
    }
    
    /**
     * Unsubscribes from a notification topic.
     * 
     * @param activity The activity context
     * @param topic The topic name
     */
    fun unsubscribeFromTopic(activity: Activity, topic: String) {
        val fcmManager = FcmTokenManager.getInstance(activity)
        fcmManager.unsubscribeFromTopic(topic)
        
        Log.d(TAG, "Unsubscribed from topic: $topic")
    }
    
    /**
     * Forces a token re-registration with the backend.
     * Useful for troubleshooting or after backend changes.
     * 
     * @param activity The activity context
     * @param onComplete Callback with success status
     */
    fun forceTokenRefresh(activity: Activity, onComplete: ((Boolean) -> Unit)? = null) {
        val fcmManager = FcmTokenManager.getInstance(activity)
        
        CoroutineScope(Dispatchers.IO).launch {
            val success = fcmManager.forceRegisterToken()
            CoroutineScope(Dispatchers.Main).launch {
                onComplete?.invoke(success)
            }
        }
    }
    
    /**
     * Checks if the app has notification permission.
     * 
     * @param activity The activity context
     * @return true if permission granted or not required, false otherwise
     */
    fun hasNotificationPermission(activity: Activity): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ActivityCompat.checkSelfPermission(
                activity,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        // Permission not required for older versions
        return true
    }
}
