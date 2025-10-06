package com.prog7314.IdleLoadsheddingMobile.networking

import com.prog7314.IdleLoadsheddingMobile.data.models.HealthResponse
import retrofit2.Response //Square, 2025.
import retrofit2.http.GET //Square, 2025.

/**
 * ApiService defines the HTTP endpoints for backend communication.
 */
interface ApiService { //Square, 2025.
    @GET("auth/ping") //Square, 2025.
    suspend fun getHealth(): Response<HealthResponse> //Square, 2025. //Kotlin Coroutines, 2025.
}

/**
 * Reference List:
 * - Square, 2025. Retrofit - Type-safe HTTP client for Android and Java. [online] Available at:
 *   https://square.github.io/retrofit/ [Accessed 1 Oct. 2025].
 * - Kotlin Coroutines, 2025. Kotlin Coroutines Guide. [online] Available at:
 *   https://kotlinlang.org/docs/coroutines-guide.html [Accessed 1 Oct. 2025].
 */
