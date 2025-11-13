package com.prog7314.IdleLoadsheddingMobile.stub

import android.app.Activity

/**
 * Stub class to replace Unity's UnityPlayer during refactoring.
 * This allows the app to compile without Unity library dependencies.
 * 
 * TEMPORARY: Remove this class and restore actual Unity integration after refactoring is complete.
 */
object UnityPlayer {
    /**
     * Returns null during stub phase - Unity is not available.
     * Real UnityPlayer.currentActivity returns the Unity activity instance.
     */
    var currentActivity: Activity? = null
    
    /**
     * Stub implementation - does nothing during refactoring.
     * Real implementation sends messages to Unity GameObjects.
     * 
     * @param gameObjectName The Unity GameObject to receive the message
     * @param methodName The method to call on the GameObject
     * @param message The message/parameter to pass
     */
    @JvmStatic
    fun UnitySendMessage(gameObjectName: String, methodName: String, message: String) {
        // Stub - no-op during refactoring
        // TODO: Remove this stub and use real Unity implementation
    }
}
