package com.prog7314.IdleLoadsheddingMobile.unity

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.prog7314.IdleLoadsheddingMobile.core.BackendClient
import com.prog7314.IdleLoadsheddingMobile.core.UnityBridge
import com.prog7314.IdleLoadsheddingMobile.data.api.StatsApi
import com.prog7314.IdleLoadsheddingMobile.data.models.BackendStatsSnapshot
import com.prog7314.IdleLoadsheddingMobile.data.models.BackendStatsSummary
import com.prog7314.IdleLoadsheddingMobile.data.models.UnityStatsSnapshot
import com.prog7314.IdleLoadsheddingMobile.utils.TapTracker
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.concurrent.CopyOnWriteArraySet

object Stats : UnityBridge.Listener, TapTracker.TapDetectionListener {

	private const val TAG = "Stats"
	private val api: StatsApi = BackendClient.create()
	private val gson = Gson()
	private val auth: FirebaseAuth = FirebaseAuth.getInstance()
	private val tapTracker = TapTracker()

	interface Observer {
		fun onStatsUpdated(snapshot: UnityStatsSnapshot)
		fun onEnergyTapped(amount: Double) {}
		fun onEnergyBurst(amount: Double) {}
	}

	private val observers = CopyOnWriteArraySet<Observer>()

	@Volatile
	var latestSnapshot: UnityStatsSnapshot? = null
		private set

	init {
		UnityBridge.registerListener(this)
		tapTracker.addListener(this)
	}

	override fun onStatsUpdated(json: String) {
		Log.d(TAG, "=== STATS UPDATE RECEIVED ===")
		Log.d(TAG, "JSON: $json")
		
		val snapshot = try {
			gson.fromJson(json, UnityStatsSnapshot::class.java)
		} catch (ex: JsonSyntaxException) {
			Log.w(TAG, "Unable to parse stats JSON", ex)
			null
		}

		if (snapshot != null) {
			Log.d(TAG, "Parsed snapshot: currentEnergy=${snapshot.currentEnergy}, totalEnergy=${snapshot.totalEnergyGenerated}")
			
			// Analyze for tap detection before updating
			tapTracker.analyzeStatsUpdate(snapshot)
			
			latestSnapshot = snapshot
			notifyObservers(snapshot)
			postSnapshot(snapshot)
		}
	}

	override fun onEnergyTapped(amount: Double) {
		Log.d(TAG, "Energy tapped: $amount")
		observers.forEach { it.onEnergyTapped(amount) }
		// Request immediate stats update from Unity after tap
		requestUnitySnapshot()
	}

	override fun onEnergyBurst(amount: Double) {
		Log.d(TAG, "Energy burst: $amount")
		observers.forEach { it.onEnergyBurst(amount) }
		// Request immediate stats update from Unity after burst
		requestUnitySnapshot()
	}

	// TapTracker.TapDetectionListener implementations
	override fun onTapDetected(amount: Double, totalTaps: Int) {
		Log.d(TAG, "Tap detected by tracker: amount=$amount, total=$totalTaps")
		observers.forEach { it.onEnergyTapped(amount) }
	}

	override fun onBurstDetected(amount: Double) {
		Log.d(TAG, "Burst detected by tracker: $amount")
		observers.forEach { it.onEnergyBurst(amount) }
	}

	override fun onRapidEnergyIncrease(increase: Double) {
		Log.d(TAG, "Rapid energy increase detected: $increase")
		// Could be used for additional UI feedback if needed
	}

	fun addObserver(observer: Observer) {
		observers.add(observer)
		latestSnapshot?.let(observer::onStatsUpdated)
	}

	fun removeObserver(observer: Observer) {
		observers.remove(observer)
	}

	private fun notifyObservers(snapshot: UnityStatsSnapshot) {
		observers.forEach { it.onStatsUpdated(snapshot) }
	}

	fun requestUnitySnapshot() {
		UnityBridge.sendMessageToUnity("UnityMessageRouter", "RequestStats", "{}")
	}

	fun fetchBackendSummary(callback: (Result<BackendStatsSummary>) -> Unit) {
		api.fetchSummary().enqueue(object : Callback<BackendStatsSummary> {
			override fun onResponse(
				call: Call<BackendStatsSummary>,
				response: Response<BackendStatsSummary>
			) {
				val body = response.body()
				if (response.isSuccessful && body != null) {
					callback(Result.success(body))
				} else {
					callback(Result.failure(Exception("Failed to fetch stats summary: ${response.code()}")))
				}
			}

			override fun onFailure(call: Call<BackendStatsSummary>, t: Throwable) {
				callback(Result.failure(t))
			}
		})
	}

	fun shutdown() {
		UnityBridge.unregisterListener(this)
		tapTracker.removeListener(this)
	}

	// Helper methods to access tap tracking data
	fun getTotalTapCount(): Int = tapTracker.getTotalTapCount()
	
	fun resetTapCounter() = tapTracker.reset()

	private fun postSnapshot(snapshot: UnityStatsSnapshot) {
		val playerId = currentPlayerId()
		if (playerId.isNullOrBlank()) {
			Log.w(TAG, "Skipping stats sync; no authenticated playerId")
			return
		}

		val request = BackendStatsSnapshot(
			playerId = playerId,
			totalEnergyGenerated = snapshot.totalEnergyGenerated,
			currentEnergy = snapshot.currentEnergy,
			energyPerSecond = snapshot.energyPerSecond,
			activeGenerators = snapshot.activeGenerators,
			generatorLevelSum = snapshot.generatorLevelSum,
			offlineSeconds = snapshot.offlineSeconds,
			lastEventId = snapshot.lastEventId,
			timestampUtc = snapshot.timestampUtc,
			currentMoney = snapshot.currentMoney,
			totalMoneyEarned = snapshot.totalMoneyEarned,
			hoursUntilMaintenance = snapshot.hoursUntilMaintenance,
			coreChargePercent = snapshot.coreChargePercent,
			districts = snapshot.districts.orEmpty()
		)

		api.postSnapshot(request).enqueue(object : Callback<Void> {
			override fun onResponse(call: Call<Void>, response: Response<Void>) {
				if (!response.isSuccessful) {
					Log.w(TAG, "Failed to push stats snapshot: ${response.code()}")
				}
			}

			override fun onFailure(call: Call<Void>, t: Throwable) {
				Log.e(TAG, "Network error posting stats", t)
			}
		})
	}

	private fun currentPlayerId(): String? = auth.currentUser?.uid
}