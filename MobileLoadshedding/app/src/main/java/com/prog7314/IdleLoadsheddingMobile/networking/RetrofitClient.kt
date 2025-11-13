package com.prog7314.IdleLoadsheddingMobile.networking

import android.content.Context
import com.prog7314.IdleLoadsheddingMobile.core.BackendClient

/**
 * RetrofitClient provides a singleton instance of the API service
 * using Retrofit for HTTP communication.
 */
object RetrofitClient { //Square, 2025.
    val instance: ApiService by lazy { //Kotlin, 2025.
        BackendClient.retrofit.create(ApiService::class.java) //Square, 2025.
    }
    
    /**
     * Creates an API service instance with authentication.
     * Used by FCM token registration.
     */
    fun createApiService(context: Context): ApiService {
        return instance
    }
}

/**
 * Reference List:
 * - Square, 2025. Retrofit - Type-safe HTTP client for Android and Java. [online] Available at:
 *   https://square.github.io/retrofit/ [Accessed 1 Oct. 2025].
 * - Kotlin, 2025. Kotlin Language Documentation - Delegated Properties. [online] Available at:
 *   https://kotlinlang.org/docs/delegated-properties.html [Accessed 1 Oct. 2025].
 */
