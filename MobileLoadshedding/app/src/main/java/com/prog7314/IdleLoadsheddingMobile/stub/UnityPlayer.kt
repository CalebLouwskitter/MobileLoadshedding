 package com.prog7314.IdleLoadsheddingMobile.stub

import android.util.Log

/**
 * TEMPORARY STUB for Unity Player - used during refactoring
 * Replace with real com.unity3d.player.UnityPlayer when Unity integration is restored
 */
object UnityPlayer {
    /**
     * Stub method - does nothing
     * Real Unity implementation would send JNI message to Unity C# side
     */
    @JvmStatic
    fun UnitySendMessage(gameObjectName: String, methodName: String, message: String) {
        Log.w("UnityPlayer", "STUB: UnitySendMessage called but not implemented - Unity integration disabled")
        Log.d("UnityPlayer", "Would send: GameObject=$gameObjectName, Method=$methodName, Message=$message")
    }
}
