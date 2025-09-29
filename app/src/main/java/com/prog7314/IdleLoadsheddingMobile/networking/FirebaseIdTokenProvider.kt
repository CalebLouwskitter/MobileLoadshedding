package com.prog7314.IdleLoadsheddingMobile.networking

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

object FirebaseIdTokenProvider {
    private const val TAG = "FirebaseIdTokenProvider"
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    suspend fun getToken(forceRefresh: Boolean = false): String? {
        val user = auth.currentUser ?: return null
        return try {
            user.getIdToken(forceRefresh).await().token
        } catch (ex: Exception) {
            Log.w(TAG, "Failed to fetch ID token", ex)
            null
        }
    }
}
