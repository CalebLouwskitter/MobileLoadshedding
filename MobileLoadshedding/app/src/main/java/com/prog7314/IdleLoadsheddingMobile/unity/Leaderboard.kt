package com.prog7314.IdleLoadsheddingMobile.unity

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.prog7314.IdleLoadsheddingMobile.core.BackendClient
import com.prog7314.IdleLoadsheddingMobile.core.UnityBridge
import com.prog7314.IdleLoadsheddingMobile.data.api.LeaderboardApi
import com.prog7314.IdleLoadsheddingMobile.data.models.LeaderboardEntryResponse
import com.prog7314.IdleLoadsheddingMobile.data.models.LeaderboardSubmission
import com.prog7314.IdleLoadsheddingMobile.data.models.UnityLeaderboardPayload
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.time.Instant
import java.util.concurrent.CopyOnWriteArraySet

object Leaderboard : UnityBridge.Listener {

	private const val TAG = "Leaderboard"

	private val gson = Gson()
	private val api: LeaderboardApi = BackendClient.create()
	private val auth: FirebaseAuth = FirebaseAuth.getInstance()

	interface Observer {
		fun onLeaderboardUpdated(payload: UnityLeaderboardPayload)
	}

	private val observers = CopyOnWriteArraySet<Observer>()

	@Volatile
	private var lastSubmittedScore: Double? = null

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
		Log.d(TAG, "fetchBackendLeaderboard() - Starting API request")
		api.fetchLeaderboard().enqueue(object : Callback<List<LeaderboardEntryResponse>> {
			override fun onResponse(
				call: Call<List<LeaderboardEntryResponse>>,
				response: Response<List<LeaderboardEntryResponse>>
			) {
				Log.d(TAG, "API Response: ${response.code()}")
				val body = response.body()
				if (response.isSuccessful && body != null) {
					Log.d(TAG, "SUCCESS: Leaderboard data received with ${body.size} entries")
					
					// Convert backend response to Unity format
					val unityPayload = UnityLeaderboardPayload(
						entries = body.map { entry ->
							UnityLeaderboardPayload.Entry(
								playerId = entry.rank.toString(), // Using rank as playerId for now
								displayName = entry.username,
								score = entry.score,
								lastUpdatedUtc = entry.recordedAt
							)
						},
						timestampUtc = Instant.now().toString()
					)
					
					latest = unityPayload
					notifyObservers(unityPayload)
					callback(Result.success(unityPayload))
				} else {
					Log.e(TAG, "FAILED: API response code ${response.code()}")
					callback(Result.failure(Exception("Failed to fetch leaderboard: ${response.code()}")))
				}
			}

			override fun onFailure(call: Call<List<LeaderboardEntryResponse>>, t: Throwable) {
				Log.e(TAG, "FAILED: Network request exception", t)
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
		val user = auth.currentUser ?: run {
			Log.d(TAG, "Skipping backend leaderboard sync - no authenticated user")
			return
		}

		val playerEntry = payload.entries.firstOrNull { it.playerId == user.uid }
			?: payload.entries.firstOrNull { entry ->
				val displayName = user.displayName
				!displayName.isNullOrBlank() && entry.displayName.equals(displayName, ignoreCase = true)
			}

		if (playerEntry == null) {
			Log.d(TAG, "No leaderboard entry found for user ${user.uid}")
			return
		}

		val previousScore = lastSubmittedScore
		if (previousScore != null && previousScore >= playerEntry.score) {
			Log.d(TAG, "Skipping leaderboard submission - score ${playerEntry.score} not higher than $previousScore")
			return
		}

		val preferredDisplayName = when {
			playerEntry.displayName.isNotBlank() -> playerEntry.displayName
			!user.displayName.isNullOrBlank() -> user.displayName!!
			!user.email.isNullOrBlank() -> user.email!!
			else -> "Player"
		}

		val submission = LeaderboardSubmission(
			playerId = user.uid,
			displayName = preferredDisplayName,
			score = playerEntry.score
		)

		api.submitScore(submission).enqueue(object : Callback<Void> {
			override fun onResponse(call: Call<Void>, response: Response<Void>) {
				if (response.isSuccessful) {
					lastSubmittedScore = playerEntry.score
					Log.d(TAG, "Leaderboard score submitted successfully")
				} else {
					Log.w(TAG, "Failed to submit leaderboard score: ${response.code()}")
				}
			}

			override fun onFailure(call: Call<Void>, t: Throwable) {
				Log.e(TAG, "Network error submitting leaderboard score", t)
			}
		})
	}
}