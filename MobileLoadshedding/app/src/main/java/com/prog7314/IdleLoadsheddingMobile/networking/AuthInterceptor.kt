package com.prog7314.IdleLoadsheddingMobile.networking

import android.util.Log //Android, 2025.
import kotlinx.coroutines.runBlocking //Kotlin Coroutines, 2025.
import okhttp3.Interceptor //Square, 2025.
import okhttp3.Response //Square, 2025.

/**
 * AuthInterceptor automatically adds Firebase authentication tokens
 * to outgoing HTTP requests.
 */
object AuthInterceptor : Interceptor { //Square, 2025.
    private const val TAG = "AuthInterceptor"
    private const val AUTH_HEADER = "Authorization"

    override fun intercept(chain: Interceptor.Chain): Response { //Square, 2025.
        val original = chain.request() //Square, 2025.
        Log.d(TAG, "Intercepting request to: ${original.url}") //Android, 2025.

        val token = try {
            runBlocking { //Kotlin Coroutines, 2025.
                Log.d(TAG, "Fetching Firebase token") //Android, 2025.
                FirebaseIdTokenProvider.getToken() 
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching token", e) //Android, 2025.
            null
        }

        val authenticatedRequest = if (!token.isNullOrBlank()) {
            Log.d(TAG, "Adding auth token to request") //Android, 2025.
            original.newBuilder() //Square, 2025.
                .header(AUTH_HEADER, "Bearer $token") //Square, 2025.
                .build()
        } else {
            Log.d(TAG, "No token available, proceeding without auth") //Android, 2025.
            original
        }

        Log.d(TAG, "Proceeding with request") //Android, 2025.
        val response = chain.proceed(authenticatedRequest) //Square, 2025.
        Log.d(TAG, "Response received: ${response.code}") //Android, 2025.
        return response
    }
}

/**
 * Reference List:
 * - Android, 2025. Android Developers - Log. [online] Available at:
 *   https://developer.android.com/reference/android/util/Log [Accessed 1 Oct. 2025].
 * - Kotlin Coroutines, 2025. Kotlin Coroutines Guide. [online] Available at:
 *   https://kotlinlang.org/docs/coroutines-guide.html [Accessed 1 Oct. 2025].
 * - Square, 2025. OkHttp - An HTTP client for Android and Java. [online] Available at:
 *   https://square.github.io/okhttp/ [Accessed 1 Oct. 2025].
 */
