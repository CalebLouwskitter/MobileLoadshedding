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
    private const val FUNCTION_NAME = "triggerNotification"

    private val functions by lazy {
        FirebaseFunctions.getInstance(BuildConfig.FIREBASE_FUNCTIONS_REGION)
    }
    private val gson = Gson()

    fun sendDemoNotification(
        type: NotificationType,
        onResult: (Boolean, String) -> Unit
    ) {
        val template = templateForType(type)
        val payload = hashMapOf(
            "targetTopic" to "all_users",
            "type" to type.name,
            "title" to template.title,
            "body" to template.body,
            "data" to template.data
        )

        functions
            .getHttpsCallable(FUNCTION_NAME)
            .call(payload)
            .addOnSuccessListener {
                Log.d(TAG, "Firebase function dispatched ${type.name}")
                onResult(true, "Cloud broadcast queued")
            }
            .addOnFailureListener { error ->
                val message = when (error) {
                    is FirebaseFunctionsException ->
                        "Firebase error (${error.code}): ${error.localizedMessage}"
                    else -> error.localizedMessage ?: "Unknown Firebase error"
                }
                Log.e(TAG, "Failed to dispatch cloud notification", error)
                onResult(false, message)
            }
    }

    private data class Template(
        val title: String,
        val body: String,
        val data: Map<String, String>
    )

    private fun templateForType(type: NotificationType): Template = when (type) {
        NotificationType.EVENT_STARTED -> Template(
            title = "⚡ New Event Started!",
            body = "Weekend Power Challenge is now live. Join now to earn bonus rewards!",
            data = mapOf(
                "type" to type.name,
                "eventId" to "event_cloud_001"
            )
        )

        NotificationType.FRIEND_REQUEST -> Template(
            title = "👋 New Friend Request",
            body = "PowerPlayer123 wants to be your friend!",
            data = mapOf(
                "type" to type.name,
                "friendId" to "cloud_friend_001"
            )
        )

        else -> Template(
            title = "📬 Idle Loadshedding",
            body = "Firebase broadcast triggered from device.",
            data = mapOf("type" to type.name)
        )
    }
}
