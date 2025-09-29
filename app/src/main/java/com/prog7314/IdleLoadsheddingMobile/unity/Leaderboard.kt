package com.prog7314.IdleLoadsheddingMobile.unity

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import java.util.concurrent.CopyOnWriteArraySet

object Leaderboard : UnityBridge.Listener {

	private const val TAG = "Leaderboard"

	private val gson = Gson()
	private val api: LeaderboardApi = BackendClient.create()

	interface Observer {
		fun onLeaderboardUpdated(payload: UnityLeaderboardPayload)
	}

	private val observers = CopyOnWriteArraySet<Observer>()

	@Volatile
	var latest: UnityLeaderboardPayload? = null
		private set

	init {
		UnityBridge.registerListener(this)
	}

	override fun onLeaderboardUpdated(json: String) {
		val payload = try {
			gson.fromJson(json, UnityLeaderboardPayload::class.java)
		} catch (ex: JsonSyntaxException) {
			Log.w(TAG, "Invalid leaderboard payload", ex)
			null
		}

		if (payload != null) {
			latest = payload
			notifyObservers(payload)
			postLeaderboard(payload)
		}
	}

	fun requestUnityLeaderboard() {
		UnityBridge.sendMessageToUnity("UnityMessageRouter", "RequestLeaderboard", "{}")
	}

	fun submitScore(playerId: String, displayName: String, score: Double) {
		val submission = LeaderboardSubmission(playerId, displayName, score)
		val json = gson.toJson(submission)
		UnityBridge.sendMessageToUnity("UnityMessageRouter", "SubmitLeaderboardScore", json)
	}

	fun fetchBackendLeaderboard(callback: (Result<UnityLeaderboardPayload>) -> Unit) {
		api.fetchLeaderboard().enqueue(object : Callback<UnityLeaderboardPayload> {
			override fun onResponse(
				call: Call<UnityLeaderboardPayload>,
				response: Response<UnityLeaderboardPayload>
			) {
				val body = response.body()
				if (response.isSuccessful && body != null) {
					latest = body
					notifyObservers(body)
					callback(Result.success(body))
				} else {
					callback(Result.failure(Exception("Failed to fetch leaderboard: ${response.code()}")))
				}
			}

			override fun onFailure(call: Call<UnityLeaderboardPayload>, t: Throwable) {
				callback(Result.failure(t))
			}
		})
	}

	fun addObserver(observer: Observer) {
		observers.add(observer)
		latest?.let(observer::onLeaderboardUpdated)
	}

	fun removeObserver(observer: Observer) {
		observers.remove(observer)
	}

	private fun notifyObservers(payload: UnityLeaderboardPayload) {
		observers.forEach { it.onLeaderboardUpdated(payload) }
	}

	private fun postLeaderboard(payload: UnityLeaderboardPayload) {
		api.postLeaderboard(payload).enqueue(object : Callback<Void> {
			override fun onResponse(call: Call<Void>, response: Response<Void>) {
				if (!response.isSuccessful) {
					Log.w(TAG, "Failed to sync leaderboard: ${response.code()}")
				}
			}

			override fun onFailure(call: Call<Void>, t: Throwable) {
				Log.e(TAG, "Network error syncing leaderboard", t)
			}
		})
	}

	interface LeaderboardApi {
		@POST("leaderboard")
		fun postLeaderboard(@Body payload: UnityLeaderboardPayload): Call<Void>

		@GET("leaderboard")
		fun fetchLeaderboard(): Call<UnityLeaderboardPayload>
	}

	data class UnityLeaderboardPayload(
		val entries: List<Entry>,
		val timestampUtc: String
	) {
		data class Entry(
			val playerId: String,
			val displayName: String,
			val score: Double,
			val lastUpdatedUtc: String
		)
	}

	data class LeaderboardSubmission(
		val playerId: String,
		val displayName: String,
		val score: Double
	)
}