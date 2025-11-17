package com.prog7314.IdleLoadsheddingMobile.networking.FirebasePush

import android.util.Log
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.gson.Gson
import com.prog7314.IdleLoadsheddingMobile.BuildConfig
import com.prog7314.IdleLoadsheddingMobile.data.models.NotificationType

/**
 * Wraps Firebase Cloud Functions calls that dispatch push notifications.
 *
 * This lets the device request a server-side broadcast without touching
 * the custom backend, enabling the "device -> Firebase -> notification" path.
 */
object FirebaseCloudMessenger {

    private const val TAG = "FirebaseCloudMessenger"

    private val functions by lazy {
        FirebaseFunctions.getInstance(BuildConfig.FIREBASE_FUNCTIONS_REGION)
    }
    private val gson = Gson()

    /**
     * Send a notification using the appropriate specialized Cloud Function
     */
    fun sendCloudNotification(
        type: NotificationType,
        onResult: (Boolean, String) -> Unit
    ) {
        val (functionName, payload) = getFunctionAndPayload(type)
        
        Log.d(TAG, "Calling Firebase function: $functionName for type: ${type.name}")
        
        functions
            .getHttpsCallable(functionName)
            .call(payload)
            .addOnSuccessListener { result ->
                Log.d(TAG, "Firebase function $functionName dispatched ${type.name}")
                val data = result.data as? Map<*, *>
                val notificationId = data?.get("notificationId") as? String ?: "unknown"
                onResult(true, "✅ Sent via $functionName")
            }
            .addOnFailureListener { error ->
                val message = when (error) {
                    is FirebaseFunctionsException ->
                        "Firebase error (${error.code}): ${error.localizedMessage}"
                    else -> error.localizedMessage ?: "Unknown Firebase error"
                }
                Log.e(TAG, "Failed to dispatch cloud notification via $functionName", error)
                onResult(false, "❌ $message")
            }
    }

    /**
     * Legacy method for backward compatibility
     */
    fun sendDemoNotification(
        type: NotificationType,
        onResult: (Boolean, String) -> Unit
    ) {
        sendCloudNotification(type, onResult)
    }

    /**
     * Get the appropriate Cloud Function name and payload for each notification type
     */
    private fun getFunctionAndPayload(type: NotificationType): Pair<String, Map<String, Any>> {
        return when (type) {
            NotificationType.EVENT_STARTED -> {
                "sendEventStarted" to mapOf(
                    "eventName" to "Weekend Power Challenge",
                    "eventId" to "event_demo_${System.currentTimeMillis()}",
                    "duration" to "48 hours",
                    "rewards" to "2x Energy + Bonus Items",
                    "targetTopic" to "all_users"
                )
            }

            NotificationType.EVENT_ENDING_SOON -> {
                "sendEventEndingSoon" to mapOf(
                    "eventName" to "Weekend Power Challenge",
                    "eventId" to "event_demo_${System.currentTimeMillis()}",
                    "timeRemaining" to "30 minutes",
                    "targetTopic" to "all_users"
                )
            }

            NotificationType.FRIEND_REQUEST -> {
                "sendFriendRequest" to mapOf(
                    "fromPlayerName" to "PowerPlayer123",
                    "fromPlayerId" to "player_demo_${System.currentTimeMillis()}",
                    "recipientToken" to "demo_token_self" // In real scenario, use actual FCM token
                )
            }

            NotificationType.FRIEND_ACCEPTED -> {
                "sendFriendAccepted" to mapOf(
                    "playerName" to "EnergyMaster456",
                    "playerId" to "player_demo_${System.currentTimeMillis()}",
                    "recipientToken" to "demo_token_self" // In real scenario, use actual FCM token
                )
            }

            NotificationType.MAINTENANCE_REQUIRED -> {
                "sendMaintenanceNotification" to mapOf(
                    "maintenanceTime" to "2025-11-20 02:00 UTC",
                    "duration" to "2 hours",
                    "targetTopic" to "all_users"
                )
            }

            NotificationType.SYSTEM_ANNOUNCEMENT -> {
                "sendSystemAnnouncement" to mapOf(
                    "title" to "New Features Available",
                    "message" to "Check out the new district upgrades and friend leaderboards!",
                    "announcementId" to "ann_demo_${System.currentTimeMillis()}",
                    "targetTopic" to "all_users"
                )
            }

            NotificationType.GENERAL -> {
                "sendGeneralNotification" to mapOf(
                    "title" to "Daily Login Bonus",
                    "body" to "You've received 1000 energy for logging in today!",
                    "data" to mapOf("bonusAmount" to "1000"),
                    "targetTopic" to "all_users"
                )
            }

            NotificationType.LEADERBOARD_UPDATE -> {
                "sendLeaderboardUpdate" to mapOf(
                    "newRank" to 5,
                    "oldRank" to 12,
                    "leaderboardName" to "Global Energy Leaders",
                    "recipientToken" to "demo_token_self" // In real scenario, use actual FCM token
                )
            }

            NotificationType.ENERGY_MILESTONE -> {
                "sendEnergyMilestone" to mapOf(
                    "milestone" to "1M Energy Generated",
                    "energyAmount" to "1,000,000 kWh",
                    "reward" to "Legendary Generator",
                    "recipientToken" to "demo_token_self" // In real scenario, use actual FCM token
                )
            }

            NotificationType.OFFLINE_EARNINGS_READY -> {
                "sendOfflineEarningsReady" to mapOf(
                    "energyEarned" to "50,000 kWh",
                    "timeAway" to "8 hours",
                    "recipientToken" to "demo_token_self" // In real scenario, use actual FCM token
                )
            }
        }
    }
}
