package com.prog7314.IdleLoadsheddingMobile.core

import android.content.Context
import android.util.Log
import com.prog7314.IdleLoadsheddingMobile.data.models.UpgradeStats
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Tiny Android plugin API that Unity calls into. It exposes synchronous getters
 * for startup, async callbacks for ongoing updates, and write methods that feed
 * straight into the Room-backed repository.
 */
object UpgradeSyncBridge {

    private const val TAG = "UpgradeSyncBridge"
    private const val UNITY_TARGET = "AndroidUpgradeStatsBridge"
    private const val UNITY_CALLBACK = "OnStatsChangedFromAndroid"
    private const val PAYLOAD_DELIMITER = "|"

    private val listeners = java.util.concurrent.CopyOnWriteArraySet<UpgradeStatsListener>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var statsJob: Job? = null

    @JvmStatic
    fun init(context: Context) {
        UpgradeStatsCoordinator.init(context)
        if (statsJob != null) return
        statsJob = scope.launch {
            UpgradeStatsCoordinator.statsState.collectLatest { stats ->
                pushToUnity(stats)
                listeners.forEach { listener ->
                    try {
                        listener.onStatsChanged(stats.damageLevel, stats.currency)
                    } catch (ex: Throwable) {
                        Log.w(TAG, "Listener threw while handling stats", ex)
                    }
                }
            }
        }
    }

    @JvmStatic
    fun shutdown() {
        statsJob?.cancel()
        statsJob = null
    }

    @JvmStatic
    fun registerListener(listener: UpgradeStatsListener) {
        listeners += listener
        val snapshot = UpgradeStatsCoordinator.statsState.value
        listener.onStatsChanged(snapshot.damageLevel, snapshot.currency)
    }

    @JvmStatic
    fun unregisterListener(listener: UpgradeStatsListener) {
        listeners -= listener
    }

    @JvmStatic
    fun getDamageLevel(): Int = UpgradeStatsCoordinator.currentDamageLevel()

    @JvmStatic
    fun getCurrency(): Long = UpgradeStatsCoordinator.currentCurrency()

    @JvmStatic
    fun buyDamageUpgrade(cost: Long, levelDelta: Int = 1) {
        scope.launch { UpgradeStatsCoordinator.buyDamageUpgrade(cost, levelDelta) }
    }

    @JvmStatic
    fun grantCurrency(amount: Long) {
        scope.launch { UpgradeStatsCoordinator.grantCurrency(amount) }
    }

    @JvmStatic
    fun overwriteFromUnity(damageLevel: Int, currency: Long) {
        scope.launch { UpgradeStatsCoordinator.overwriteFromUnity(damageLevel, currency) }
    }

    @JvmStatic
    fun requestImmediatePush() {
        pushToUnity(UpgradeStatsCoordinator.statsState.value)
    }

    private fun pushToUnity(stats: UpgradeStats) {
        val payload = buildString {
            append(stats.damageLevel)
            append(PAYLOAD_DELIMITER)
            append(stats.currency)
        }
        UnityBridge.sendMessageToUnity(UNITY_TARGET, UNITY_CALLBACK, payload)
    }
}

fun interface UpgradeStatsListener {
    fun onStatsChanged(damageLevel: Int, currency: Long)
}
