package com.prog7314.IdleLoadsheddingMobile.utils

import android.util.Log
import com.prog7314.IdleLoadsheddingMobile.core.BackendClient
import com.prog7314.IdleLoadsheddingMobile.data.api.FriendsApi
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * Service to resolve short friend codes back to Firebase UIDs.
 * Uses the backend API to resolve codes that have been stored during registration.
 */
object FriendCodeResolver {
    
    private const val TAG = "FriendCodeResolver"
    private val api: FriendsApi = BackendClient.create()
    
    /**
     * Resolves a short friend code to a Firebase UID using the backend API.
     * 
     * @param friendCode The 8-character friend code (can be formatted with dash or not)
     * @return The Firebase UID if found, null otherwise
     */
    suspend fun resolveFriendCode(friendCode: String): String? {
        return try {
            val cleanCode = FriendCodeGenerator.removeFormatting(friendCode)
            
            if (!FriendCodeGenerator.isValidFriendCodeFormat(cleanCode)) {
                Log.w(TAG, "Invalid friend code format: $friendCode")
                return null
            }
            
            // Call backend API to resolve the friend code
            suspendCancellableCoroutine { continuation ->
                api.resolveFriendCode(cleanCode).enqueue(object : Callback<Map<String, String>> {
                    override fun onResponse(
                        call: Call<Map<String, String>>,
                        response: Response<Map<String, String>>
                    ) {
                        if (response.isSuccessful) {
                            val firebaseUid = response.body()?.get("FirebaseUid")
                            Log.d(TAG, "Resolved friend code $cleanCode to user ID: $firebaseUid")
                            continuation.resume(firebaseUid)
                        } else {
                            Log.w(TAG, "No user found with friend code: $cleanCode (${response.code()})")
                            continuation.resume(null)
                        }
                    }
                    
                    override fun onFailure(call: Call<Map<String, String>>, t: Throwable) {
                        Log.e(TAG, "Error resolving friend code: $friendCode", t)
                        continuation.resume(null)
                    }
                })
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error resolving friend code: $friendCode", e)
            null
        }
    }
}
