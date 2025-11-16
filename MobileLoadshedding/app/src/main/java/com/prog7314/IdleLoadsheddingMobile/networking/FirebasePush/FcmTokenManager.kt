package com.prog7314.IdleLoadsheddingMobile.networking.FirebasePush

import android.content.Context
import android.provider.Settings
import android.util.Log
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.prog7314.IdleLoadsheddingMobile.BuildConfig
import com.prog7314.IdleLoadsheddingMobile.data.models.FcmTokenRequest
import com.prog7314.IdleLoadsheddingMobile.networking.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.Instant

private const val FCM_DATASTORE_NAME = "fcm_preferences"

val Context.fcmPreferencesDataStore by preferencesDataStore(name = FCM_DATASTORE_NAME)

/**
 * Manages Firebase Cloud Messaging (FCM) tokens for push notifications.
 * 
 * This class:
 * - Retrieves FCM tokens from Firebase
 * - Stores tokens locally using DataStore
 * - Registers tokens with the backend API
 * - Handles token refresh events
 * 
 * Follows the same pattern as AuthPreferences for consistency.
 * 
 * References:
 * - Firebase Cloud Messaging, 2025. FCM Token Management.
 *   https://firebase.google.com/docs/cloud-messaging/android/client#retrieve-the-current-registration-token
 * - Android Jetpack, 2025. DataStore Preferences.
 *   https://developer.android.com/topic/libraries/architecture/datastore
 */
class FcmTokenManager private constructor(private val context: Context) {

    companion object {
        private const val TAG = "FcmTokenManager"
        
        @Volatile
        private var instance: FcmTokenManager? = null
        
        fun getInstance(context: Context): FcmTokenManager {
            return instance ?: synchronized(this) {
                instance ?: FcmTokenManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val firestore by lazy { FirebaseFirestore.getInstance() }

    private object Keys {
        val fcmToken: Preferences.Key<String> = stringPreferencesKey("fcm_token")
        val lastRegisteredToken: Preferences.Key<String> = stringPreferencesKey("last_registered_token")
        val lastRegistrationTime: Preferences.Key<String> = stringPreferencesKey("last_registration_time")
        val deviceId: Preferences.Key<String> = stringPreferencesKey("device_id")
    }

    /**
     * Current FCM token stored locally.
     */
    val fcmToken: Flow<String?> = context.fcmPreferencesDataStore.data.map { preferences ->
        preferences[Keys.fcmToken]
    }

    /**
     * Last token that was successfully registered with the backend.
     */
    val lastRegisteredToken: Flow<String?> = context.fcmPreferencesDataStore.data.map { preferences ->
        preferences[Keys.lastRegisteredToken]
    }

    /**
     * Device ID used to identify this device in backend.
     */
    private val deviceId: Flow<String> = context.fcmPreferencesDataStore.data.map { preferences ->
        preferences[Keys.deviceId] ?: generateDeviceId()
    }

    /**
     * Initializes FCM and retrieves the current token.
     * Should be called when the app starts and user is authenticated.
     */
    fun initializeFcm() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Get current Firebase user
                val currentUser = FirebaseAuth.getInstance().currentUser
                if (currentUser == null) {
                    Log.w(TAG, "User not authenticated, skipping FCM initialization")
                    return@launch
                }

                // Ensure device ID is stored
                ensureDeviceId()

                // Retrieve FCM token
                val token = FirebaseMessaging.getInstance().token.await()
                Log.d(TAG, "FCM token retrieved: ${token.take(20)}...")

                // Store token locally
                saveFcmToken(token)
                persistTokenInFirebase(token)

                // Register with backend if token has changed
                registerTokenWithBackend(token)

            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize FCM", e)
            }
        }
    }

    /**
     * Called when FCM generates a new token.
     * This is triggered by FirebaseMessagingService.onNewToken()
     */
    fun refreshToken(newToken: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "Refreshing FCM token: ${newToken.take(20)}...")
                
                // Store new token
                saveFcmToken(newToken)
                persistTokenInFirebase(newToken)
                
                // Register with backend
                registerTokenWithBackend(newToken)
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to refresh FCM token", e)
            }
        }
    }

    /**
     * Manually triggers token registration with backend.
     * Useful for retry logic or manual sync.
     */
    suspend fun forceRegisterToken(): Boolean {
        return try {
            val token = fcmToken.first()
            if (token.isNullOrBlank()) {
                Log.w(TAG, "No FCM token available to register")
                return false
            }
            
            registerTokenWithBackend(token)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to force register token", e)
            false
        }
    }

    /**
     * Checks if the current token needs to be registered with the backend.
     * Returns true if token has changed since last registration.
     */
    suspend fun needsRegistration(): Boolean {
        val current = fcmToken.first()
        val lastRegistered = lastRegisteredToken.first()
        return current != null && current != lastRegistered
    }

    /**
     * Saves FCM token to local DataStore.
     */
    private suspend fun saveFcmToken(token: String) {
        context.fcmPreferencesDataStore.edit { preferences ->
            preferences[Keys.fcmToken] = token
        }
        Log.d(TAG, "FCM token saved locally")
    }

    /**
     * Registers the FCM token with the backend API.
     */
    private suspend fun registerTokenWithBackend(token: String) {
        if (!BuildConfig.ENABLE_BACKEND_TOKEN_REGISTRATION) {
            Log.d(TAG, "Skipping backend registration (disabled via BuildConfig)")
            markTokenRegistered(token)
            return
        }

        try {
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser == null) {
                Log.w(TAG, "User not authenticated, cannot register token")
                return
            }

            // Check if already registered
            val lastRegistered = lastRegisteredToken.first()
            if (token == lastRegistered) {
                Log.d(TAG, "Token already registered with backend")
                return
            }

            val devId = deviceId.first()
            val request = FcmTokenRequest(
                firebaseUid = currentUser.uid,
                fcmToken = token,
                deviceId = devId,
                platform = "android",
                timestampUtc = Instant.now().toString()
            )

            // Make API call to register token
            val apiService = RetrofitClient.createApiService(context)
            val response = apiService.registerFcmToken(request)

            if (response.isSuccessful) {
                markTokenRegistered(token)
                Log.d(TAG, "FCM token successfully registered with backend")
                persistTokenInFirebase(token)
            } else {
                Log.e(TAG, "Failed to register FCM token: ${response.code()} ${response.message()}")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error registering FCM token with backend", e)
        }
    }

    private suspend fun markTokenRegistered(token: String) {
        context.fcmPreferencesDataStore.edit { preferences ->
            preferences[Keys.lastRegisteredToken] = token
            preferences[Keys.lastRegistrationTime] = Instant.now().toString()
        }
    }

    private suspend fun persistTokenInFirebase(token: String) {
        try {
            val currentUser = FirebaseAuth.getInstance().currentUser
            val deviceIdentifier = deviceId.first()
            val documentId = currentUser?.uid ?: deviceIdentifier
            val payload = hashMapOf(
                "token" to token,
                "firebaseUid" to currentUser?.uid,
                "deviceId" to deviceIdentifier,
                "platform" to "android",
                "updatedAt" to FieldValue.serverTimestamp()
            )

            firestore.collection("fcmTokens")
                .document(documentId)
                .set(payload)
                .await()

            Log.d(TAG, "Token stored in Firestore for $documentId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to store token in Firestore", e)
        }
    }

    /**
     * Ensures a device ID exists in DataStore.
     */
    private suspend fun ensureDeviceId() {
        val existingId = deviceId.first()
        if (existingId.isBlank()) {
            val newId = generateDeviceId()
            context.fcmPreferencesDataStore.edit { preferences ->
                preferences[Keys.deviceId] = newId
            }
            Log.d(TAG, "Generated new device ID: $newId")
        }
    }

    /**
     * Generates a unique device ID.
     */
    private fun generateDeviceId(): String {
        return try {
            // Use Android ID as device identifier
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
                ?: "device_${System.currentTimeMillis()}"
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get Android ID, using timestamp", e)
            "device_${System.currentTimeMillis()}"
        }
    }

    /**
     * Clears all FCM data from DataStore.
     * Should be called on logout.
     */
    suspend fun clearFcmData() {
        context.fcmPreferencesDataStore.edit { preferences ->
            preferences.remove(Keys.fcmToken)
            preferences.remove(Keys.lastRegisteredToken)
            preferences.remove(Keys.lastRegistrationTime)
            // Keep device ID as it's device-specific, not user-specific
        }
        Log.d(TAG, "FCM data cleared")
    }

    /**
     * Unsubscribes from FCM topics and clears data.
     * Should be called on logout.
     */
    fun unsubscribeFromNotifications() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Unsubscribe from any topics the user might be subscribed to
                FirebaseMessaging.getInstance().deleteToken().await()
                
                // Clear local data
                clearFcmData()
                
                Log.d(TAG, "Successfully unsubscribed from notifications")
            } catch (e: Exception) {
                Log.e(TAG, "Error unsubscribing from notifications", e)
            }
        }
    }

    /**
     * Subscribes to a notification topic.
     * Topics allow sending notifications to groups of users.
     */
    fun subscribeToTopic(topic: String) {
        FirebaseMessaging.getInstance().subscribeToTopic(topic)
            .addOnSuccessListener {
                Log.d(TAG, "Successfully subscribed to topic: $topic")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to subscribe to topic: $topic", e)
            }
    }

    /**
     * Unsubscribes from a notification topic.
     */
    fun unsubscribeFromTopic(topic: String) {
        FirebaseMessaging.getInstance().unsubscribeFromTopic(topic)
            .addOnSuccessListener {
                Log.d(TAG, "Successfully unsubscribed from topic: $topic")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to unsubscribe from topic: $topic", e)
            }
    }
}
