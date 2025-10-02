package com.prog7314.IdleLoadsheddingMobile.unity

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.prog7314.IdleLoadsheddingMobile.core.BackendClient
import com.prog7314.IdleLoadsheddingMobile.core.UnityBridge
import com.prog7314.IdleLoadsheddingMobile.data.api.FriendsApi
import com.prog7314.IdleLoadsheddingMobile.data.models.FriendProfile
import com.prog7314.IdleLoadsheddingMobile.data.models.FriendResponseDto
import com.prog7314.IdleLoadsheddingMobile.data.models.FriendSubmission
import com.prog7314.IdleLoadsheddingMobile.data.models.FriendsPayload
import java.time.Instant
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.concurrent.CopyOnWriteArraySet

object FriendsList : UnityBridge.Listener {
    private const val TAG = "FriendsList"

    private val gson = Gson()
    private val api: FriendsApi = BackendClient.create()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    interface Observer {
        fun onFriendsUpdated(payload: FriendsPayload)
    }

    private val observers = CopyOnWriteArraySet<Observer>()

    @Volatile
    var latest: FriendsPayload? = null
        private set

    init {
        UnityBridge.registerListener(this)
    }

    fun addObserver(observer: Observer) {
        observers.add(observer)
        latest?.let(observer::onFriendsUpdated)
    }

    fun removeObserver(observer: Observer) {
        observers.remove(observer)
    }

    override fun onFriendsUpdated(json: String) {
        val payload = try {
            gson.fromJson(json, FriendsPayload::class.java)
        } catch (ex: JsonSyntaxException) {
            Log.w(TAG, "Invalid friends payload", ex)
            null
        }

        if (payload != null) {
            latest = payload
            notifyObservers(payload)
            postFriends(payload)
        }
    }

    fun requestUnityFriends() {
        UnityBridge.sendMessageToUnity("UnityMessageRouter", "RequestFriends", "{}")
    }

    fun updateFriend(id: String, displayName: String, contributionPerHour: Double, online: Boolean) {
        val submission = FriendSubmission(id, displayName, contributionPerHour, online, remove = false)
        UnityBridge.sendMessageToUnity("UnityMessageRouter", "SubmitFriendUpdate", gson.toJson(submission))
    }

    fun removeFriend(id: String) {
        val submission = FriendSubmission(id, displayName = "", contributionPerHour = 0.0, isOnline = false, remove = true)
        UnityBridge.sendMessageToUnity("UnityMessageRouter", "SubmitFriendUpdate", gson.toJson(submission))
    }

    fun fetchBackendFriends(callback: (Result<FriendsPayload>) -> Unit) {
        api.fetchFriends(currentPlayerId()).enqueue(object : Callback<List<FriendResponseDto>> {
            override fun onResponse(
                call: Call<List<FriendResponseDto>>,
                response: Response<List<FriendResponseDto>>
            ) {
                if (response.isSuccessful) {
                    val friends = response.body().orEmpty().map { it.toFriendProfile() }
                    val payload = FriendsPayload(
                        friends = friends,
                        timestampUtc = Instant.now().toString()
                    )

                    latest = payload
                    notifyObservers(payload)
                    callback(Result.success(payload))
                } else {
                    callback(Result.failure(Exception("Failed to fetch friends: ${response.code()}")))
                }
            }

            override fun onFailure(call: Call<List<FriendResponseDto>>, t: Throwable) {
                callback(Result.failure(t))
            }
        })
    }

    private fun postFriends(payload: FriendsPayload) {
        api.postFriends(currentPlayerId(), payload).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (!response.isSuccessful) {
                    Log.w(TAG, "Failed to sync friends: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.e(TAG, "Network error syncing friends", t)
            }
        })
    }

    private fun notifyObservers(payload: FriendsPayload) {
        observers.forEach { it.onFriendsUpdated(payload) }
    }

    private fun FriendResponseDto.toFriendProfile(): FriendProfile = FriendProfile(
        id = userId.toString(),
        displayName = username,
        isOnline = false,
        contributionPerHour = 0.0,
        lastInteractionUtc = addedAt
    )

    private fun currentPlayerId(): String? = auth.currentUser?.uid
}