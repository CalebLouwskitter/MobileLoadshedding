package com.prog7314.IdleLoadsheddingMobile.networking

import android.util.Log //Android, 2025.
import com.google.firebase.auth.FirebaseAuth //Firebase, 2025.
import kotlinx.coroutines.tasks.await //Kotlin Coroutines, 2025.

/**
 * FirebaseIdTokenProvider manages retrieval of Firebase authentication ID tokens
 * for authenticated API requests.
 */
object FirebaseIdTokenProvider { //Firebase, 2025.
    private const val TAG = "FirebaseIdTokenProvider"
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() } //Firebase, 2025.

    suspend fun getToken(forceRefresh: Boolean = false): String? { //Kotlin Coroutines, 2025.
        val user = auth.currentUser ?: return null //Firebase, 2025.
        return try {
            user.getIdToken(forceRefresh).await().token //Firebase, 2025. //Kotlin Coroutines, 2025.
        } catch (ex: Exception) {
            Log.w(TAG, "Failed to fetch ID token", ex) //Android, 2025.
            null
        }
    }
}

/**
 * Reference List:
 * - Android, 2025. Android Developers - Log. [online] Available at:
 *   https://developer.android.com/reference/android/util/Log [Accessed 1 Oct. 2025].
 * - Firebase, 2025. Firebase Authentication for Android. [online] Available at:
 *   https://firebase.google.com/docs/auth/android/start [Accessed 1 Oct. 2025].
 * - Kotlin Coroutines, 2025. Kotlin Coroutines Guide. [online] Available at:
 *   https://kotlinlang.org/docs/coroutines-guide.html [Accessed 1 Oct. 2025].
 */
