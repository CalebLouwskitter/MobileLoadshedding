package com.prog7314.IdleLoadsheddingMobile.networking

import com.prog7314.IdleLoadsheddingMobile.data.models.FcmTokenRequest
import com.prog7314.IdleLoadsheddingMobile.data.models.FcmTokenResponse
import com.prog7314.IdleLoadsheddingMobile.data.models.GameSyncRequest
import com.prog7314.IdleLoadsheddingMobile.data.models.GameSyncResponse
import com.prog7314.IdleLoadsheddingMobile.data.models.HealthResponse
import com.prog7314.IdleLoadsheddingMobile.data.models.NotificationActionRequest
import com.prog7314.IdleLoadsheddingMobile.data.models.NotificationPreferencesResponse
import com.prog7314.IdleLoadsheddingMobile.data.models.UpdateNotificationPreferencesRequest
import com.prog7314.IdleLoadsheddingMobile.data.models.UpgradeRequest
import com.prog7314.IdleLoadsheddingMobile.data.models.UpgradeResponse
import retrofit2.Response //Square, 2025.
import retrofit2.http.Body
import retrofit2.http.GET //Square, 2025.
import retrofit2.http.POST
import retrofit2.http.PUT

/**
 * ApiService defines the HTTP endpoints for backend communication.
 */
interface ApiService { //Square, 2025.
    @GET("auth/ping") //Square, 2025.
    suspend fun getHealth(): Response<HealthResponse> //Square, 2025. //Kotlin Coroutines, 2025.
    
    // Game sync endpoints
    
    /**
     * Synchronize game state with server and calculate offline earnings.
     * Called when app resumes from background.
     */
    @POST("game/sync")
    suspend fun syncGameState(@Body request: GameSyncRequest): Response<GameSyncResponse>
    
    /**
     * Validate and purchase an upgrade with server-side validation.
     * Called when player purchases an upgrade in Unity.
     */
    @POST("game/upgrade")
    suspend fun purchaseUpgrade(@Body request: UpgradeRequest): Response<UpgradeResponse>
    
    // Firebase Cloud Messaging endpoints
    
    /**
     * Register or update FCM token for push notifications.
     * Called when FCM token is generated or refreshed.
     */
    @POST("notifications/register-token")
    suspend fun registerFcmToken(@Body request: FcmTokenRequest): Response<FcmTokenResponse>
    
    /**
     * Track notification action (opened, dismissed, etc.) for analytics.
     */
    @POST("notifications/action")
    suspend fun trackNotificationAction(@Body request: NotificationActionRequest): Response<Unit>
    
    /**
     * Get notification preferences for the current user.
     */
    @GET("notifications/preferences")
    suspend fun getNotificationPreferences(): Response<NotificationPreferencesResponse>
    
    /**
     * Update notification preferences for the current user.
     */
    @PUT("notifications/preferences")
    suspend fun updateNotificationPreferences(@Body request: UpdateNotificationPreferencesRequest): Response<Unit>
}

/**
 * Reference List:
 * - Square, 2025. Retrofit - Type-safe HTTP client for Android and Java. [online] Available at:
 *   https://square.github.io/retrofit/ [Accessed 1 Oct. 2025].
 * - Kotlin Coroutines, 2025. Kotlin Coroutines Guide. [online] Available at:
 *   https://kotlinlang.org/docs/coroutines-guide.html [Accessed 1 Oct. 2025].
 */
