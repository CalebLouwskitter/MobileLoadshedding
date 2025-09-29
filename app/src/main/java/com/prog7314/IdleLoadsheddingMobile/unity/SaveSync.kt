package com.prog7314.IdleLoadsheddingMobile.unity

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import java.util.concurrent.CopyOnWriteArraySet

object SaveSync : UnityBridge.Listener {

	private const val TAG = "SaveSync"
	private const val PREFS_FILE = "idle_save_store"
	private const val PREF_KEY_SAVE = "latest_save"

	private val gson = Gson()
	private val api: SaveApi = BackendClient.create()
	private val auth: FirebaseAuth = FirebaseAuth.getInstance()

		interface Listener {
			fun onSaveUpdated(payload: UnitySavePayload)
		}

		private val listeners = CopyOnWriteArraySet<Listener>()

		@Volatile
		var latestSave: UnitySavePayload? = null
			private set

	private var prefs: SharedPreferences? = null

	init {
		UnityBridge.registerListener(this)
	}

	fun initialize(context: Context) {
		prefs = context.applicationContext.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
		restoreCachedSave()?.let {
			latestSave = it
			notifyListeners(it)
		}
	}

	fun requestUnitySave() {
		UnityBridge.sendMessageToUnity("UnityMessageRouter", "RequestSave", "{}")
	}

	fun pushSaveToUnity(payload: UnitySavePayload) {
		val json = gson.toJson(payload)
		UnityBridge.sendMessageToUnity("UnityMessageRouter", "SubmitSave", json)
	}

	override fun onSaveSynced(json: String) {
		val payload = try {
			gson.fromJson(json, UnitySavePayload::class.java)
		} catch (ex: JsonSyntaxException) {
			Log.w(TAG, "Invalid save payload", ex)
			null
		}

		if (payload != null) {
			latestSave = payload
			persist(json)
			notifyListeners(payload)
			postSave(payload)
		}
	}

	fun addListener(listener: Listener) {
		listeners.add(listener)
		latestSave?.let(listener::onSaveUpdated)
	}

	fun removeListener(listener: Listener) {
		listeners.remove(listener)
	}

	private fun persist(json: String) {
		prefs?.edit { putString(PREF_KEY_SAVE, json) }
	}

	fun restoreCachedSave(): UnitySavePayload? {
		val json = prefs?.getString(PREF_KEY_SAVE, null) ?: return null
		return runCatching { gson.fromJson(json, UnitySavePayload::class.java) }.getOrNull()
	}

	private fun notifyListeners(payload: UnitySavePayload) {
		listeners.forEach { it.onSaveUpdated(payload) }
	}

	private fun postSave(payload: UnitySavePayload) {
		val playerId = currentPlayerId()
		if (playerId.isNullOrBlank()) {
			Log.w(TAG, "Skipping save sync; no authenticated playerId")
			return
		}

		val request = BackendSavePayload(
			playerId = playerId,
			stats = payload.stats,
			generators = payload.generators.map { BackendGeneratorState(it.id, it.level) },
			districts = payload.districts.map { BackendDistrictState(it.id, it.level, it.efficiency) },
			central = payload.central?.let { BackendCentralState(it.charge) },
			timestampUtc = payload.timestampUtc
		)

		api.syncSave(request).enqueue(object : Callback<Void> {
			override fun onResponse(call: Call<Void>, response: Response<Void>) {
				if (!response.isSuccessful) {
					Log.w(TAG, "Failed to sync save: ${response.code()}")
				}
			}

			override fun onFailure(call: Call<Void>, t: Throwable) {
				Log.e(TAG, "Network error syncing save", t)
			}
		})
	}

	private fun currentPlayerId(): String? = auth.currentUser?.uid

	interface SaveApi {
		@POST("save")
		fun syncSave(@Body payload: BackendSavePayload): Call<Void>
	}

	data class UnitySavePayload(
		val stats: String,
		val generators: List<GeneratorState>,
		val timestampUtc: String,
		val districts: List<DistrictState> = emptyList(),
		val central: CentralState? = null
	) {
		data class GeneratorState(
			val id: String,
			val level: Int
		)

		data class DistrictState(
			val id: String,
			val level: Int,
			val efficiency: Float
		)

		data class CentralState(
			val charge: Double
		)
	}

	data class BackendSavePayload(
		val playerId: String,
		val stats: String,
		val generators: List<BackendGeneratorState>,
		val districts: List<BackendDistrictState> = emptyList(),
		val central: BackendCentralState? = null,
		val timestampUtc: String
	)

	data class BackendGeneratorState(
		val id: String,
		val level: Int
	)

	data class BackendDistrictState(
		val id: String,
		val level: Int,
		val efficiency: Float
	)

	data class BackendCentralState(
		val charge: Double
	)
}