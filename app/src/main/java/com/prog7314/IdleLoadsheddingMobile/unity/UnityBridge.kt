package com.prog7314.IdleLoadsheddingMobile.unity

import com.prog7314.IdleLoadsheddingMobile.core.UnityBridge as CoreUnityBridge

/**
 * Backwards-compatible bridge exposed under the legacy package path expected by the Unity project.
 * Delegates every static call to the new implementation in `core.UnityBridge`.
 */
class UnityBridge private constructor() {

    companion object {

        @JvmStatic
        fun updateScore(score: String) = CoreUnityBridge.updateScore(score)

        @JvmStatic
        fun notifyGameEvent(event: String) = CoreUnityBridge.notifyGameEvent(event)

        @JvmStatic
        fun updateStats(json: String) = CoreUnityBridge.updateStats(json)

        @JvmStatic
        fun syncSaveData(json: String) = CoreUnityBridge.syncSaveData(json)

        @JvmStatic
        fun updateLeaderboard(json: String) = CoreUnityBridge.updateLeaderboard(json)

        @JvmStatic
        fun updateFriends(json: String) = CoreUnityBridge.updateFriends(json)

        @JvmStatic
        fun sendMessageToUnity(gameObject: String, methodName: String, message: String) =
            CoreUnityBridge.sendMessageToUnity(gameObject, methodName, message)

        @JvmStatic
        fun sendJsonToUnity(gameObject: String, methodName: String, json: String) =
            CoreUnityBridge.sendJsonToUnity(gameObject, methodName, json)
    }
}

/**
 * Reference List:
 * - Kotlin, 2025. Kotlin Documentation - Java Interop. [online] Available at:
 *   https://kotlinlang.org/docs/java-interop.html [Accessed 2 Oct. 2025].
 */