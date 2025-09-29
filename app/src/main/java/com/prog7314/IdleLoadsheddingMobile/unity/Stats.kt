package com.prog7314.IdleLoadsheddingMobile.unity

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.annotations.SerializedName
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import java.util.concurrent.CopyOnWriteArraySet

object Stats : UnityBridge.Listener {

	private const val TAG = "Stats"
	private val api: StatsApi = BackendClient.create()
	private val gson = Gson()
	private val auth: FirebaseAuth = FirebaseAuth.getInstance()

	interface Observer {
		fun onStatsUpdated(snapshot: UnityStatsSnapshot)
	}

	private val observers = CopyOnWriteArraySet<Observer>()

	@Volatile
	var latestSnapshot: UnityStatsSnapshot? = null
		private set

	init {
		UnityBridge.registerListener(this)
	}

	override fun onStatsUpdated(json: String) {
		val snapshot = try {
			gson.fromJson(json, UnityStatsSnapshot::class.java)
		} catch (ex: JsonSyntaxException) {
			Log.w(TAG, "Unable to parse stats JSON", ex)
			null
		}

		if (snapshot != null) {
			latestSnapshot = snapshot
			notifyObservers(snapshot)
			postSnapshot(snapshot)
		}
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
	}

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
			districts = snapshot.districts
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

	interface StatsApi {
		@POST("stats")
		fun postSnapshot(@Body snapshot: BackendStatsSnapshot): Call<Void>

		@GET("stats/summary")
		fun fetchSummary(): Call<BackendStatsSummary>
	}

	data class UnityStatsSnapshot(
		@SerializedName("totalEnergyGenerated") val totalEnergyGenerated: Double,
		@SerializedName("currentEnergy") val currentEnergy: Double,
		@SerializedName("energyPerSecond") val energyPerSecond: Double,
		@SerializedName("activeGenerators") val activeGenerators: Int,
		@SerializedName("generatorLevelSum") val generatorLevelSum: Int,
		@SerializedName("offlineSeconds") val offlineSeconds: Double,
		@SerializedName("lastEventId") val lastEventId: String?,
		@SerializedName("timestampUtc") val timestampUtc: String,
		@SerializedName("currentMoney") val currentMoney: Double = 0.0,
		@SerializedName("totalMoneyEarned") val totalMoneyEarned: Double = 0.0,
		@SerializedName("hoursUntilMaintenance") val hoursUntilMaintenance: Double = 0.0,
		@SerializedName("coreChargePercent") val coreChargePercent: Double = 0.0,
		@SerializedName("districts") val districts: List<DistrictSnapshot> = emptyList()
	) {
		data class DistrictSnapshot(
			@SerializedName("id") val id: String,
			@SerializedName("name") val name: String,
			@SerializedName("level") val level: Int,
			@SerializedName("efficiency") val efficiency: Double,
			@SerializedName("powerPerSecond") val powerPerSecond: Double,
			@SerializedName("moneyPerSecond") val moneyPerSecond: Double,
			@SerializedName("hoursUntilMaintenance") val hoursUntilMaintenance: Double,
			@SerializedName("nextUpgradeCost") val nextUpgradeCost: Double
		)
	}

	data class BackendStatsSnapshot(
		@SerializedName("playerId") val playerId: String,
		@SerializedName("totalEnergyGenerated") val totalEnergyGenerated: Double,
		@SerializedName("currentEnergy") val currentEnergy: Double,
		@SerializedName("energyPerSecond") val energyPerSecond: Double,
		@SerializedName("activeGenerators") val activeGenerators: Int,
		@SerializedName("generatorLevelSum") val generatorLevelSum: Int,
		@SerializedName("offlineSeconds") val offlineSeconds: Double,
		@SerializedName("lastEventId") val lastEventId: String?,
		@SerializedName("timestampUtc") val timestampUtc: String,
		@SerializedName("currentMoney") val currentMoney: Double = 0.0,
		@SerializedName("totalMoneyEarned") val totalMoneyEarned: Double = 0.0,
		@SerializedName("hoursUntilMaintenance") val hoursUntilMaintenance: Double = 0.0,
		@SerializedName("coreChargePercent") val coreChargePercent: Double = 0.0,
		@SerializedName("districts") val districts: List<UnityStatsSnapshot.DistrictSnapshot> = emptyList()
	)

	data class BackendStatsSummary(
		val totalPlayers: Int,
		val averageEnergyPerSecond: Double,
		val topPerformers: List<Performer> = emptyList()
	) {
		data class Performer(
			val playerId: String,
			val displayName: String,
			val score: Double
		)
	}
}