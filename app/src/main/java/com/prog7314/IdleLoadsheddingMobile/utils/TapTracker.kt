package com.prog7314.IdleLoadsheddingMobile.utils

import android.util.Log
import com.prog7314.IdleLoadsheddingMobile.data.models.UnityStatsSnapshot
import java.util.concurrent.CopyOnWriteArraySet

/**
 * TapTracker analyzes energy changes to detect tap events and energy bursts
 * by monitoring rapid increases in energy that don't match the expected passive generation rate.
 */
class TapTracker {
    
    companion object {
        private const val TAG = "TapTracker"
        // Simplified thresholds - any energy increase above passive generation
        private const val MIN_TAP_ENERGY = 1.0 // Minimum energy to consider
        private const val RAPID_UPDATE_THRESHOLD_MS = 1000L // Updates within 1 second are likely taps
    }
    
    interface TapDetectionListener {
        fun onTapDetected(amount: Double, totalTaps: Int)
        fun onBurstDetected(amount: Double)
        fun onRapidEnergyIncrease(increase: Double)
    }
    
    private val listeners = CopyOnWriteArraySet<TapDetectionListener>()
    private var lastSnapshot: UnityStatsSnapshot? = null
    private var lastUpdateTime = 0L
    private var totalTapCount = 0
    private var recentEnergyIncreases = mutableListOf<Pair<Long, Double>>()
    
    fun addListener(listener: TapDetectionListener) {
        listeners.add(listener)
    }
    
    fun removeListener(listener: TapDetectionListener) {
        listeners.remove(listener)
    }
    
    fun analyzeStatsUpdate(newSnapshot: UnityStatsSnapshot) {
        val currentTime = System.currentTimeMillis()
        val previousSnapshot = lastSnapshot
        
        if (previousSnapshot != null) {
            val energyIncrease = newSnapshot.currentEnergy - previousSnapshot.currentEnergy
            val timeDiff = currentTime - lastUpdateTime
            
            Log.d(TAG, "Stats update: energyIncrease=$energyIncrease, timeDiff=${timeDiff}ms")
            
            // Detect tap: any positive energy increase that happens quickly
            if (energyIncrease > MIN_TAP_ENERGY && timeDiff < RAPID_UPDATE_THRESHOLD_MS) {
                totalTapCount++
                recentEnergyIncreases.add(Pair(currentTime, energyIncrease))
                
                Log.d(TAG, "TAP DETECTED! Amount: $energyIncrease, Total taps: $totalTapCount")
                
                // Notify listeners
                listeners.forEach { it.onTapDetected(energyIncrease, totalTapCount) }
                listeners.forEach { it.onRapidEnergyIncrease(energyIncrease) }
                
                // Check if it's a burst (larger amount)
                if (energyIncrease >= 10.0) {
                    listeners.forEach { it.onBurstDetected(energyIncrease) }
                }
            } else if (energyIncrease > 0) {
                Log.d(TAG, "Passive energy increase: $energyIncrease over ${timeDiff}ms")
            }
        } else {
            Log.d(TAG, "First stats update received")
        }
        
        lastSnapshot = newSnapshot
        lastUpdateTime = currentTime
        
        // Clean up old entries
        cleanupOldEntries(currentTime)
    }
    
    private fun cleanupOldEntries(currentTime: Long) {
        recentEnergyIncreases.removeAll { (timestamp, _) -> 
            currentTime - timestamp > RAPID_UPDATE_THRESHOLD_MS * 2
        }
    }
    
    fun getTotalTapCount() = totalTapCount
    
    fun getRecentEnergyIncreases() = recentEnergyIncreases.toList()
    
    fun reset() {
        lastSnapshot = null
        lastUpdateTime = 0L
        totalTapCount = 0
        recentEnergyIncreases.clear()
    }
}

/**
 * Reference List:
 * - Android, 2025. Android Developers - Util Package. [online] Available at:
 *   https://developer.android.com/reference/android/util/package-summary [Accessed 4 Oct. 2025].
 * - Kotlin, 2025. Kotlin Documentation - Collections. [online] Available at:
 *   https://kotlinlang.org/docs/collections-overview.html [Accessed 4 Oct. 2025].
 */