package com.prog7314.IdleLoadsheddingMobile.core

import android.content.Context //Android, 2025.
import android.os.Handler //Android, 2025.
import android.os.Looper //Android, 2025.
import android.util.Log //Android, 2025.
import android.widget.Toast //Android, 2025.
import com.unity3d.player.UnityPlayer //Unity Technologies, 2025.
import org.json.JSONObject //Android, 2025.
import java.util.UUID //Java, 2025.
import java.util.concurrent.ConcurrentHashMap //Java, 2025.
import java.util.concurrent.ConcurrentLinkedQueue //Java, 2025.
import java.util.concurrent.CopyOnWriteArraySet //Java, 2025.
import java.util.concurrent.atomic.AtomicBoolean //Java, 2025.

/**
 * UnityBridge provides communication between Unity engine and Android native code.
 */
object UnityBridge {

    private const val TAG = "UnityBridge"
    private const val UNITY_BRIDGE_OBJECT = "AndroidBridge"
    private const val UNITY_METHOD_UPGRADE = "RequestUpgrade"
    private const val UNITY_METHOD_PING = "PingFromAndroid"
    private const val PING_TIMEOUT_MS = 2_000L

    // Listener for events coming from Unity (C# -> Android UI)
    interface Listener {
        fun onScoreUpdated(score: Int) {}
        fun onGameEvent(event: String) {}
        fun onStatsUpdated(json: String) {}
        fun onSaveSynced(json: String) {}
        fun onCheckpointReached(json: String) {}
        fun onLeaderboardUpdated(json: String) {}
        fun onFriendsUpdated(json: String) {}
        fun onEnergyTapped(amount: Double) {}
        fun onEnergyBurst(amount: Double) {}
        fun onUpgradeApplied(payloadJson: String) {}
        fun onPingResult(success: Boolean) {}
    }

    private val listeners = CopyOnWriteArraySet<Listener>() //Java, 2025.
    private val mainHandler = Handler(Looper.getMainLooper()) //Android, 2025.
    private val unityReady = AtomicBoolean(false)
    private val pendingUnityCalls = ConcurrentLinkedQueue<() -> Unit>()
    private val pingCallbacks = ConcurrentHashMap<String, (Boolean) -> Unit>()
    @Volatile private var unityHudVisible = true

    fun registerListener(l: Listener) {
        listeners.add(l)
    }

    fun unregisterListener(l: Listener) {
        listeners.remove(l)
    }

    // Called by Unity C# via: new AndroidJavaClass("com.prog7314.IdleLoadsheddingMobile.core.UnityBridge").CallStatic("updateScore", scoreString)
    @JvmStatic //Kotlin, 2025.
    fun updateScore(score: String) {
        Log.d(TAG, "updateScore called with: $score")
        markUnityReady()
        val value = score.toIntOrNull() ?: return
        mainHandler.post { //Android, 2025.
            listeners.forEach { it.onScoreUpdated(value) }
        }
    }

    // Called by Unity C# via: new AndroidJavaClass("com.prog7314.IdleLoadsheddingMobile.core.UnityBridge").CallStatic("notifyGameEvent", evt)
    @JvmStatic //Kotlin, 2025.
    fun notifyGameEvent(event: String) {
        Log.d(TAG, "notifyGameEvent called with: $event")
        markUnityReady()
        mainHandler.post { //Android, 2025.
            listeners.forEach { it.onGameEvent(event) }
        }
    }

    @JvmStatic //Kotlin, 2025.
    fun updateStats(statsJson: String) {
        Log.d(TAG, "updateStats called with: $statsJson")
        markUnityReady()
        mainHandler.post { //Android, 2025.
            listeners.forEach { it.onStatsUpdated(statsJson) }
        }
    }

    @JvmStatic //Kotlin, 2025.
    fun syncSaveData(saveJson: String) {
        Log.d(TAG, "syncSaveData called with: $saveJson")
        markUnityReady()
        mainHandler.post { //Android, 2025.
            listeners.forEach { it.onSaveSynced(saveJson) }
        }
    }

    @JvmStatic //Kotlin, 2025.
    fun syncCheckpoint(checkpointJson: String) {
        Log.d(TAG, "syncCheckpoint called with: $checkpointJson")
        markUnityReady()
        mainHandler.post { //Android, 2025.
            listeners.forEach { it.onCheckpointReached(checkpointJson) }
        }
    }

    @JvmStatic //Kotlin, 2025.
    fun updateLeaderboard(json: String) {
        Log.d(TAG, "updateLeaderboard called with: $json")
        markUnityReady()
        mainHandler.post { //Android, 2025.
            listeners.forEach { it.onLeaderboardUpdated(json) }
        }
    }

    @JvmStatic //Kotlin, 2025.
    fun updateFriends(json: String) {
        Log.d(TAG, "updateFriends called with: $json")
        markUnityReady()
        mainHandler.post { //Android, 2025.
            listeners.forEach { it.onFriendsUpdated(json) }
        }
    }

    // Called by Unity C# when energy tap occurs
    @JvmStatic //Kotlin, 2025.
    fun notifyEnergyTap(amountString: String) {
        Log.d(TAG, "notifyEnergyTap called with: $amountString")
        markUnityReady()
        val amount = amountString.toDoubleOrNull() ?: return
        mainHandler.post { //Android, 2025.
            listeners.forEach { it.onEnergyTapped(amount) }
        }
    }

    // Called by Unity C# when burst energy is added
    @JvmStatic //Kotlin, 2025.
    fun notifyEnergyBurst(amountString: String) {
        Log.d(TAG, "notifyEnergyBurst called with: $amountString")
        markUnityReady()
        val amount = amountString.toDoubleOrNull() ?: return
        mainHandler.post { //Android, 2025.
            listeners.forEach { it.onEnergyBurst(amount) }
        }
    }

    // Utility for local UI toast
    fun showToast(context: Context, message: String) { //Android, 2025.
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show() //Android, 2025.
    }

    // Android UI -> Unity (Kotlin -> C# MonoBehaviour method with one string parameter)
    fun sendMessageToUnity(gameObject: String, methodName: String, message: String) {
        dispatchUnityMessage(gameObject, methodName, message)
    }

    // Convenience for sending JSON payloads as strings
    fun sendJsonToUnity(gameObject: String, methodName: String, json: String) {
        dispatchUnityMessage(gameObject, methodName, json)
    }

    // Convenience helpers to control Unity-side HUD visibility from Android UI layer
    fun hideUnityHud() {
        unityHudVisible = false
        sendMessageToUnity("AndroidUIManager", "HideDebugUIManually", "")
    }

    fun showUnityHud() {
        unityHudVisible = true
        sendMessageToUnity("AndroidUIManager", "ShowDebugUI", "")
    }

    fun toggleUnityHud() {
        if (unityHudVisible) {
            hideUnityHud()
        } else {
            showUnityHud()
        }
    }

    fun sendUpgradeRequest(upgradeId: String, newLevel: Int, cost: Int) {
        val payload = JSONObject().apply {
            put("upgradeId", upgradeId)
            put("level", newLevel)
            put("cost", cost)
            put("timestamp", System.currentTimeMillis())
        }.toString()
        dispatchUnityMessage(UNITY_BRIDGE_OBJECT, UNITY_METHOD_UPGRADE, payload)
    }

    fun pingUnity(onResult: ((Boolean) -> Unit)? = null) {
        val nonce = UUID.randomUUID().toString()
        pingCallbacks[nonce] = { success ->
            onResult?.invoke(success)
            notifyPingResult(success)
        }
        dispatchUnityMessage(UNITY_BRIDGE_OBJECT, UNITY_METHOD_PING, nonce)
        mainHandler.postDelayed({
            pingCallbacks.remove(nonce)?.invoke(false)
        }, PING_TIMEOUT_MS)
    }

    private fun dispatchUnityMessage(gameObject: String, methodName: String, payload: String) {
        enqueueWhenReady {
            val activity = UnityPlayer.currentActivity
            if (activity == null) {
                Log.w(TAG, "Unity activity null while sending $gameObject.$methodName; proceeding on main thread")
            }

            val targets = buildTargetList(gameObject)
            var delivered = false
            var lastError: Throwable? = null

            for (target in targets) {
                try {
                    UnityPlayer.UnitySendMessage(target, methodName, payload)
                    if (target != gameObject) {
                        Log.d(TAG, "Routed Unity message to fallback target '$target' for method '$methodName'")
                    }
                    delivered = true
                    break
                } catch (ex: Throwable) {
                    lastError = ex
                }
            }

            if (!delivered) {
                Log.w(TAG, "Failed to send message to Unity object '$gameObject.$methodName'", lastError)
            }
        }
    }

    private fun buildTargetList(primary: String): List<String> {
        // Unity clones instantiated prefabs with "(Clone)" suffix; try both names automatically.
        return if (primary.endsWith("(Clone)")) {
            listOf(primary)
        } else {
            listOf(primary, "$primary(Clone)")
        }
    }

    @JvmStatic //Kotlin, 2025.
    fun onUnityReady(unused: String = "") {
        Log.d(TAG, "Unity signaled ready")
        markUnityReady()
    }

    @JvmStatic //Kotlin, 2025.
    fun onPingResponse(nonce: String) {
        Log.d(TAG, "Ping response for $nonce")
        markUnityReady()
        pingCallbacks.remove(nonce)?.invoke(true) ?: notifyPingResult(true)
    }

    @JvmStatic //Kotlin, 2025.
    fun onUpgradeApplied(payloadJson: String) {
        Log.d(TAG, "Upgrade applied: $payloadJson")
        markUnityReady()
        mainHandler.post {
            listeners.forEach { it.onUpgradeApplied(payloadJson) }
        }
    }

    private fun notifyPingResult(success: Boolean) {
        mainHandler.post {
            listeners.forEach { it.onPingResult(success) }
        }
    }

    private fun enqueueWhenReady(action: () -> Unit) {
        if (unityReady.get()) {
            mainHandler.post { action() }
        } else {
            pendingUnityCalls.add(action)
        }
    }

    @Synchronized
    private fun markUnityReady() {
        if (unityReady.compareAndSet(false, true)) {
            Log.d(TAG, "Unity bridge marked ready, flushing ${pendingUnityCalls.size} queued calls")
        }
        drainPendingCalls()
    }

    private fun drainPendingCalls() {
        while (true) {
            val action = pendingUnityCalls.poll() ?: break
            mainHandler.post { action() }
        }
    }
}

/**
 * Reference List:
 * - Android, 2025. Android Developers Documentation. [online] Available at:
 *   https://developer.android.com/ [Accessed 1 Oct. 2025].
 * - Unity Technologies, 2025. Unity for Android - Native Plugin Integration. [online] Available at:
 *   https://docs.unity3d.com/Manual/android-plugins.html [Accessed 1 Oct. 2025].
 * - Java, 2025. Java Platform Standard Edition Documentation. [online] Available at:
 *   https://docs.oracle.com/en/java/javase/ [Accessed 1 Oct. 2025].
 * - Kotlin, 2025. Kotlin Documentation - Java Interop. [online] Available at:
 *   https://kotlinlang.org/docs/java-interop.html [Accessed 1 Oct. 2025].
 */
