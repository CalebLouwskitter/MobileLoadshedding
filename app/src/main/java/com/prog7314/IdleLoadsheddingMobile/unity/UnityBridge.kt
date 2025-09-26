package com.prog7314.IdleLoadsheddingMobile.unity

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.unity3d.player.UnityPlayer

object UnityBridge {

    // Listener for events coming from Unity (C# -> Android UI)
    interface Listener {
        fun onScoreUpdated(score: Int) {}
        fun onGameEvent(event: String) {}
    }

    @Volatile
    private var listener: Listener? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    fun registerListener(l: Listener) {
        listener = l
    }

    fun unregisterListener(l: Listener) {
        if (listener === l) listener = null
    }

    // Called by Unity C# via: new AndroidJavaClass("com.prog7314.IdleLoadsheddingMobile.unity.UnityBridge").CallStatic("updateScore", scoreString)
    @JvmStatic
    fun updateScore(score: String) {
        val value = score.toIntOrNull() ?: return
        mainHandler.post { listener?.onScoreUpdated(value) }
    }

    // Called by Unity C# via: new AndroidJavaClass("com.prog7314.IdleLoadsheddingMobile.unity.UnityBridge").CallStatic("notifyGameEvent", evt)
    @JvmStatic
    fun notifyGameEvent(event: String) {
        mainHandler.post { listener?.onGameEvent(event) }
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
