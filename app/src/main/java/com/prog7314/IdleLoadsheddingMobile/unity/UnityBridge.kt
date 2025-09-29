package com.prog7314.IdleLoadsheddingMobile.unity

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.unity3d.player.UnityPlayer
import java.util.concurrent.CopyOnWriteArraySet

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

    private val listeners = CopyOnWriteArraySet<Listener>()
    private val mainHandler = Handler(Looper.getMainLooper())

    fun registerListener(l: Listener) {
        listeners.add(l)
    }

    fun unregisterListener(l: Listener) {
        listeners.remove(l)
    }

    // Called by Unity C# via: new AndroidJavaClass("com.prog7314.IdleLoadsheddingMobile.unity.UnityBridge").CallStatic("updateScore", scoreString)
    @JvmStatic
    fun updateScore(score: String) {
        val value = score.toIntOrNull() ?: return
        mainHandler.post {
            listeners.forEach { it.onScoreUpdated(value) }
        }
    }

    // Called by Unity C# via: new AndroidJavaClass("com.prog7314.IdleLoadsheddingMobile.unity.UnityBridge").CallStatic("notifyGameEvent", evt)
    @JvmStatic
    fun notifyGameEvent(event: String) {
        mainHandler.post {
            listeners.forEach { it.onGameEvent(event) }
        }
    }

    @JvmStatic
    fun updateStats(statsJson: String) {
        mainHandler.post {
            listeners.forEach { it.onStatsUpdated(statsJson) }
        }
    }

    @JvmStatic
    fun syncSaveData(saveJson: String) {
        mainHandler.post {
            listeners.forEach { it.onSaveSynced(saveJson) }
        }
    }

    @JvmStatic
    fun updateLeaderboard(json: String) {
        mainHandler.post {
            listeners.forEach { it.onLeaderboardUpdated(json) }
        }
    }

    @JvmStatic
    fun updateFriends(json: String) {
        mainHandler.post {
            listeners.forEach { it.onFriendsUpdated(json) }
        }
    }

    // Utility for local UI toast
    fun showToast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    // Android UI -> Unity (Kotlin -> C# MonoBehaviour method with one string parameter)
    fun sendMessageToUnity(gameObject: String, methodName: String, message: String) {
        UnityPlayer.UnitySendMessage(gameObject, methodName, message)
    }

    // Convenience for sending JSON payloads as strings
    fun sendJsonToUnity(gameObject: String, methodName: String, json: String) {
        UnityPlayer.UnitySendMessage(gameObject, methodName, json)
    }
}
