package com.prog7314.IdleLoadsheddingMobile.ui.coordinators

import android.util.Log
import com.prog7314.IdleLoadsheddingMobile.data.models.*
import com.prog7314.IdleLoadsheddingMobile.unity.*

/**
 * Coordinates all Unity module observers to reduce MainActivity bloat.
 * 
 * This class centralizes observer registration, data distribution, and cleanup.
 * Instead of MainActivity having 5 separate observer objects, it only needs
 * to interact with this single coordinator.
 * 
 * WHY THIS EXISTS:
 * - Reduces MainActivity from 5 observer declarations to 1 coordinator reference
 * - Centralizes observer lifecycle management (subscribe/unsubscribe)
 * - Provides single point for debugging Unity callbacks
 * - Makes testing easier by isolating observer logic
 */
class ObserverCoordinator(
    private val stateManager: StateManager,
    private val onStateChanged: () -> Unit
) {
    
    // Internal observers that forward data to StateManager
    
    private val statsObserver = object : Stats.Observer {
        override fun onStatsUpdated(snapshot: UnityStatsSnapshot) {
            stateManager.updateStats(snapshot)
            onStateChanged()
        }
    }
    
    private val leaderboardObserver = object : Leaderboard.Observer {
        override fun onLeaderboardUpdated(payload: UnityLeaderboardPayload) {
            Log.d("ObserverCoordinator", "Leaderboard updated with ${payload.entries.size} entries")
            stateManager.updateLeaderboard(payload)
            onStateChanged()
        }
    }
    
    private val friendsObserver = object : FriendsList.Observer {
        override fun onFriendsUpdated(payload: FriendsPayload) {
            stateManager.updateFriends(payload)
            onStateChanged()
        }
    }
    
    private val eventsObserver = object : Events.Observer {
        override fun onEventsUpdated(activeEvent: UnityEventPayload?, history: List<UnityEventPayload>) {
            stateManager.updateEvents(activeEvent, history)
            onStateChanged()
        }
    }
    
    private val saveListener = object : SaveSync.Listener {
        override fun onSaveUpdated(payload: UnitySavePayload) {
            stateManager.updateSave(payload)
            onStateChanged()
        }
    }
    
    /**
     * Subscribe to all Unity module observers.
     * Call this after Unity is ready but before requesting initial data.
     */
    fun subscribeAll() {
        Stats.addObserver(statsObserver)
        Leaderboard.addObserver(leaderboardObserver)
        FriendsList.addObserver(friendsObserver)
        Events.addObserver(eventsObserver)
        SaveSync.addListener(saveListener)
    }
    
    /**
     * Unsubscribe from all Unity module observers.
     * Call this in onDestroy to prevent memory leaks.
     */
    fun unsubscribeAll() {
        Stats.removeObserver(statsObserver)
        Leaderboard.removeObserver(leaderboardObserver)
        FriendsList.removeObserver(friendsObserver)
        Events.removeObserver(eventsObserver)
        SaveSync.removeListener(saveListener)
    }
    
    /**
     * Request initial data from all Unity modules.
     * This triggers the first round of observer callbacks.
     */
    fun requestInitialData() {
        Log.d("ObserverCoordinator", "Requesting initial data from Unity modules")
        Stats.requestUnitySnapshot()
        
        // Fetch leaderboard directly from API
        Leaderboard.fetchBackendLeaderboard { result ->
            result.onSuccess { payload ->
                Log.d("ObserverCoordinator", "Leaderboard fetched: ${payload.entries.size} entries")
            }.onFailure { error ->
                Log.e("ObserverCoordinator", "Leaderboard fetch failed: ${error.message}", error)
                Leaderboard.requestUnityLeaderboard()
            }
        }
        
        // Fetch friends directly from API
        FriendsList.fetchBackendFriends { result ->
            result.onSuccess { payload ->
                Log.d("ObserverCoordinator", "Friends fetched: ${payload.friends.size} friends")
            }.onFailure { error ->
                Log.e("ObserverCoordinator", "Friends fetch failed: ${error.message}", error)
                FriendsList.requestUnityFriends()
            }
        }
        
        SaveSync.requestUnitySave()
    }
}
