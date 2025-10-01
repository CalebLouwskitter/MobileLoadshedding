package com.prog7314.IdleLoadsheddingMobile.core

import android.content.Context //Android, 2025.
import android.os.Handler //Android, 2025.
import android.os.Looper //Android, 2025.
import android.widget.Toast //Android, 2025.
import com.unity3d.player.UnityPlayer //Unity Technologies, 2025.
import java.util.concurrent.CopyOnWriteArraySet //Java, 2025.

/**
 * UnityBridge provides communication between Unity engine and Android native code.
 */
object UnityBridge {

    // Listener for events coming from Unity (C# -> Android UI)
    interface Listener {
        fun onScoreUpdated(score: Int) {}
        fun onGameEvent(event: String) {}
        fun onStatsUpdated(json: String) {}
        fun onSaveSynced(json: String) {}
        fun onLeaderboardUpdated(json: String) {}
        fun onFriendsUpdated(json: String) {}
    }

    private val listeners = CopyOnWriteArraySet<Listener>() //Java, 2025.
    private val mainHandler = Handler(Looper.getMainLooper()) //Android, 2025.

    fun registerListener(l: Listener) {
        listeners.add(l)
    }

    fun unregisterListener(l: Listener) {
        listeners.remove(l)
    }

    // Called by Unity C# via: new AndroidJavaClass("com.prog7314.IdleLoadsheddingMobile.core.UnityBridge").CallStatic("updateScore", scoreString)
    @JvmStatic //Kotlin, 2025.
    fun updateScore(score: String) {
        val value = score.toIntOrNull() ?: return
        mainHandler.post { //Android, 2025.
            listeners.forEach { it.onScoreUpdated(value) }
        }
    }

    // Called by Unity C# via: new AndroidJavaClass("com.prog7314.IdleLoadsheddingMobile.core.UnityBridge").CallStatic("notifyGameEvent", evt)
    @JvmStatic //Kotlin, 2025.
    fun notifyGameEvent(event: String) {
        mainHandler.post { //Android, 2025.
            listeners.forEach { it.onGameEvent(event) }
        }
    }

    @JvmStatic //Kotlin, 2025.
    fun updateStats(statsJson: String) {
        mainHandler.post { //Android, 2025.
            listeners.forEach { it.onStatsUpdated(statsJson) }
        }
    }

    @JvmStatic //Kotlin, 2025.
    fun syncSaveData(saveJson: String) {
        mainHandler.post { //Android, 2025.
            listeners.forEach { it.onSaveSynced(saveJson) }
        }
    }

    @JvmStatic //Kotlin, 2025.
    fun updateLeaderboard(json: String) {
        mainHandler.post { //Android, 2025.
            listeners.forEach { it.onLeaderboardUpdated(json) }
        }
    }

    @JvmStatic //Kotlin, 2025.
    fun updateFriends(json: String) {
        mainHandler.post { //Android, 2025.
            listeners.forEach { it.onFriendsUpdated(json) }
        }
    }

    // Utility for local UI toast
    fun showToast(context: Context, message: String) { //Android, 2025.
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show() //Android, 2025.
    }

    // Android UI -> Unity (Kotlin -> C# MonoBehaviour method with one string parameter)
    fun sendMessageToUnity(gameObject: String, methodName: String, message: String) {
        UnityPlayer.UnitySendMessage(gameObject, methodName, message) //Unity Technologies, 2025.
    }

    // Convenience for sending JSON payloads as strings
    fun sendJsonToUnity(gameObject: String, methodName: String, json: String) {
        UnityPlayer.UnitySendMessage(gameObject, methodName, json) //Unity Technologies, 2025.
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
