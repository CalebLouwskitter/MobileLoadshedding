package com.prog7314.IdleLoadsheddingMobile.unity

import android.util.Log
import com.google.gson.Gson
import com.prog7314.IdleLoadsheddingMobile.core.UnityBridge

/**
 * PowerManager handles power generation and upgrade actions.
 * It sends commands to Unity's ResourceGenerator GameObjects.
 */
object PowerManager {
    
    private const val TAG = "PowerManager"
    private val gson = Gson()
    
    /**
     * Request Unity to click/tap a power generator for burst energy
     */
    fun clickGenerator(generatorId: String) {
        Log.d(TAG, "Requesting generator click: $generatorId")
        val payload = gson.toJson(mapOf("generatorId" to generatorId))
        UnityBridge.sendMessageToUnity("UnityMessageRouter", "ClickGenerator", payload)
    }
    
    /**
     * Request Unity to upgrade a generator to the next level
     */
    fun upgradeGenerator(generatorId: String) {
        Log.d(TAG, "Requesting generator upgrade: $generatorId")
        val payload = gson.toJson(mapOf("generatorId" to generatorId))
        UnityBridge.sendMessageToUnity("UnityMessageRouter", "UpgradeGenerator", payload)
    }
    
    /**
     * Request Unity to send current generator states
     */
    fun requestGeneratorStates() {
        Log.d(TAG, "Requesting generator states from Unity")
        UnityBridge.sendMessageToUnity("UnityMessageRouter", "RequestGeneratorStates", "{}")
    }
    
    /**
     * Request Unity to upgrade a district
     */
    fun upgradeDistrict(districtId: String) {
        Log.d(TAG, "Requesting district upgrade: $districtId")
        val payload = gson.toJson(mapOf("districtId" to districtId))
        UnityBridge.sendMessageToUnity("UnityMessageRouter", "UpgradeDistrict", payload)
    }

    /**
     * Request Unity to service a district (reset efficiency/maintenance)
     */
    fun serviceDistrict(districtId: String) {
        Log.d(TAG, "Requesting district service: $districtId")
        val payload = gson.toJson(mapOf("districtId" to districtId))
        UnityBridge.sendMessageToUnity("UnityMessageRouter", "ServiceDistrict", payload)
    }
}

/**
 * Reference List:
 * - Kotlin, 2025. Kotlin Documentation. [online] Available at:
 *   https://kotlinlang.org/docs/ [Accessed 2 Oct. 2025].
 * - Google, 2025. Gson Documentation. [online] Available at:
 *   https://github.com/google/gson [Accessed 2 Oct. 2025].
 */
