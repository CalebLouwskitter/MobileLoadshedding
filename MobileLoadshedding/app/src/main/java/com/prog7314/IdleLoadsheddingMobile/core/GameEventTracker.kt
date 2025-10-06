package com.prog7314.IdleLoadsheddingMobile.core

import android.content.Context
import android.util.Log
import com.prog7314.IdleLoadsheddingMobile.unity.Stats

/**
 * GameEventTracker provides additional tracking for game events
 * and can be extended to monitor specific Unity events if needed.
 */
object GameEventTracker {
    
    private const val TAG = "GameEventTracker"
    
    // Called by Unity when button is explicitly tapped (if Unity is modified later)
    @JvmStatic
    fun notifyButtonTap(buttonType: String, amount: String) {
        val tapAmount = amount.toDoubleOrNull() ?: return
        Log.d(TAG, "Button tap: type=$buttonType, amount=$tapAmount")
        
        // This would directly trigger our tap events
        UnityBridge.notifyEnergyTap(amount)
    }
    
    // Called by Unity for any significant game action
    @JvmStatic
    fun notifyGameAction(actionType: String, parameters: String) {
        Log.d(TAG, "Game action: type=$actionType, params=$parameters")
        
        when (actionType) {
            "energy_tap" -> {
                try {
                    val amount = parameters.toDouble()
                    UnityBridge.notifyEnergyTap(parameters)
                } catch (e: NumberFormatException) {
                    Log.w(TAG, "Invalid energy amount: $parameters")
                }
            }
            "energy_burst" -> {
                try {
                    val amount = parameters.toDouble()
                    UnityBridge.notifyEnergyBurst(parameters)
                } catch (e: NumberFormatException) {
                    Log.w(TAG, "Invalid burst amount: $parameters")
                }
            }
            else -> {
                Log.d(TAG, "Unknown action type: $actionType")
            }
        }
    }
    
    // Utility methods for future expansion
    fun getTotalTaps(): Int = Stats.getTotalTapCount()
    
    fun resetCounters() = Stats.resetTapCounter()
    
    fun getCurrentStats() = Stats.latestSnapshot
}

/**
 * Reference List:
 * - Android, 2025. Android Developers - Util Package. [online] Available at:
 *   https://developer.android.com/reference/android/util/package-summary [Accessed 4 Oct. 2025].
 * - Kotlin, 2025. Kotlin Documentation - Object Declarations. [online] Available at:
 *   https://kotlinlang.org/docs/object-declarations.html [Accessed 4 Oct. 2025].
 */