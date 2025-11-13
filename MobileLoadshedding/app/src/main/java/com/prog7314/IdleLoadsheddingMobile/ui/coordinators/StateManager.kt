package com.prog7314.IdleLoadsheddingMobile.ui.coordinators

import android.util.Log
import com.prog7314.IdleLoadsheddingMobile.data.models.*

/**
 * Centralized state manager for all game data caches.
 * 
 * This class consolidates the 7+ scattered state variables in MainActivity
 * into a single, organized container. It provides thread-safe access to
 * cached game state and tracks what data has been loaded.
 * 
 * WHY THIS EXISTS:
 * - Eliminates scattered lateinit vars and nullable state in MainActivity
 * - Provides single source of truth for current game state
 * - Makes it easy to clear state on logout
 * - Simplifies state inspection during debugging
 * - Enables future features like state persistence or undo/redo
 */
class StateManager {
    
    // Core game state
    private var _latestStats: UnityStatsSnapshot? = null
    val latestStats: UnityStatsSnapshot? get() = _latestStats
    
    private var _latestLeaderboard: UnityLeaderboardPayload? = null
    val latestLeaderboard: UnityLeaderboardPayload? get() = _latestLeaderboard
    
    private var _latestFriends: FriendsPayload? = null
    val latestFriends: FriendsPayload? get() = _latestFriends
    
    private var _latestSave: UnitySavePayload? = null
    val latestSave: UnitySavePayload? get() = _latestSave
    
    private var _currentActiveEvent: UnityEventPayload? = null
    val currentActiveEvent: UnityEventPayload? get() = _currentActiveEvent
    
    private var _eventHistory: List<UnityEventPayload> = emptyList()
    val eventHistory: List<UnityEventPayload> get() = _eventHistory
    
    // Generator change tracking for animations
    // Maps generatorId -> Pair(previousLevel, previousOutput)
    private val _previousGeneratorStats = mutableMapOf<String, Pair<Int, Double>>()
    val previousGeneratorStats: Map<String, Pair<Int, Double>> get() = _previousGeneratorStats
    
    /**
     * Update stats snapshot and track generator changes.
     */
    fun updateStats(snapshot: UnityStatsSnapshot) {
        // Track district changes before updating (districts are like generators)
        snapshot.districts?.forEach { district ->
            _previousGeneratorStats[district.id] = Pair(district.level, district.powerPerSecond)
        }
        _latestStats = snapshot
    }
    
	/**
	 * Update leaderboard data.
	 */
	fun updateLeaderboard(payload: UnityLeaderboardPayload) {
		Log.d("StateManager", "💾 Updating leaderboard state (${payload.entries.size} entries)")
		_latestLeaderboard = payload
	}    /**
     * Update friends list.
     */
    fun updateFriends(payload: FriendsPayload) {
        _latestFriends = payload
    }
    
    /**
     * Update event state (active event + history).
     */
    fun updateEvents(activeEvent: UnityEventPayload?, history: List<UnityEventPayload>) {
        _currentActiveEvent = activeEvent
        _eventHistory = history
    }
    
    /**
     * Update save payload.
     */
    fun updateSave(payload: UnitySavePayload) {
        _latestSave = payload
    }
    
    /**
     * Clear all state (e.g., on logout).
     */
    fun clearAll() {
        _latestStats = null
        _latestLeaderboard = null
        _latestFriends = null
        _latestSave = null
        _currentActiveEvent = null
        _eventHistory = emptyList()
        _previousGeneratorStats.clear()
    }
    
    /**
     * Check if we have received initial game data.
     */
    fun hasInitialData(): Boolean {
        return _latestStats != null
    }
    
    /**
     * Get current energy/money for display.
     * Prioritizes money > energy > total generated.
     */
    fun getCurrentCredits(): Double {
        val stats = _latestStats ?: return 0.0
        return when {
            stats.currentMoney > 0.0 -> stats.currentMoney
            stats.currentEnergy > 0.0 -> stats.currentEnergy
            else -> stats.totalEnergyGenerated
        }
    }
}
