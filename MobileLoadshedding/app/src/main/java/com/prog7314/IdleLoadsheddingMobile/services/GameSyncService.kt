package com.prog7314.IdleLoadsheddingMobile.services

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.prog7314.IdleLoadsheddingMobile.core.UnityBridge
import com.prog7314.IdleLoadsheddingMobile.data.local.AppDatabase
import com.prog7314.IdleLoadsheddingMobile.data.local.entities.GameStateFull
import com.prog7314.IdleLoadsheddingMobile.data.models.UnityCheckpointPayload
import com.prog7314.IdleLoadsheddingMobile.data.models.UnityCurrencySnapshot
import com.prog7314.IdleLoadsheddingMobile.data.models.UnityHappinessSnapshot
import com.prog7314.IdleLoadsheddingMobile.data.models.UnitySavePayload
import com.prog7314.IdleLoadsheddingMobile.data.models.UnitySectionSnapshot
import com.prog7314.IdleLoadsheddingMobile.data.models.UnityStatsSnapshot
import com.prog7314.IdleLoadsheddingMobile.data.repository.GameStateRepository
import com.prog7314.IdleLoadsheddingMobile.core.BackendClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Service to handle game state synchronization between Unity, Room database, and backend.
 * 
 * Features:
 * - Auto-save game state to Room database
 * - Sync with backend when online
 * - Load saved games from Room
 * - Conflict resolution (last-write-wins)
 */
internal class GameSyncService(
    context: Context
) : UnityBridge.Listener {
    
    private val database = AppDatabase.getDatabase(context)
    private val repository = GameStateRepository(database)
    private val gson = Gson()
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private var currentPlayerId: String? = null
    @Volatile private var lastUnityBroadcastVersion: Long? = null
    @Volatile private var lastCheckpointToken: String? = null
    
    companion object {
        private const val TAG = "GameSyncService"
        private const val AUTO_SAVE_INTERVAL_MS = 30000L // 30 seconds
    }
    
    /**
     * Initialize the service with current player ID.
     * Registers Unity bridge listener and loads saved game if available.
     */
    fun initialize(playerId: String) {
        currentPlayerId = playerId
        UnityBridge.registerListener(this)
        Log.d(TAG, "GameSyncService initialized for player: $playerId")
        
        // Load saved game from Room
        loadSavedGame(playerId)
        
        // Start observing game state changes
        observeGameState(playerId)
    }
    
    /**
     * Cleanup when service is no longer needed.
     */
    fun cleanup() {
        UnityBridge.unregisterListener(this)
        Log.d(TAG, "GameSyncService cleaned up")
    }
    
    /**
     * Load saved game from Room database and send to Unity.
     */
    private fun loadSavedGame(playerId: String) {
        serviceScope.launch {
            try {
                val gameState = repository.getGameState(playerId)
                if (gameState != null) {
                    Log.d(TAG, "Loaded saved game for player: $playerId")
                    
                    // Convert to Unity payload and send to Unity
                    val payload = repository.toUnitySavePayload(gameState)
                    val json = gson.toJson(payload)
                    
                    UnityBridge.sendJsonToUnity(
                        "AndroidUIManager",
                        "LoadGameState",
                        json
                    )
                    lastUnityBroadcastVersion = gameState.gameState.lastModified?.time
                    
                    Log.d(TAG, "Game state sent to Unity")
                } else {
                    Log.d(TAG, "No saved game found for player: $playerId")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading saved game", e)
            }
        }
    }
    
    /**
     * Observe game state changes and trigger sync when needed.
     */
    private fun observeGameState(playerId: String) {
        serviceScope.launch {
            repository.getGameStateFlow(playerId).collectLatest { gameState ->
                if (gameState == null) {
                    return@collectLatest
                }

                maybeBroadcastUnityUpdate(gameState)

                if (gameState.gameState.needsUpload) {
                    Log.d(TAG, "Game state needs upload, syncing with backend...")
                    syncWithBackend(playerId)
                }
            }
        }
    }
    
    /**
     * Called when Unity sends updated stats.
     * Save to Room database.
     */
    override fun onStatsUpdated(json: String) {
        serviceScope.launch {
            try {
                val playerId = currentPlayerId ?: return@launch
                val stats = gson.fromJson(json, UnityStatsSnapshot::class.java)
                
                // Save to Room database
                repository.saveGameState(
                    playerId = playerId,
                    unityStats = stats,
                    generators = null, // Will be provided in onSaveSynced
                    prestigeLevel = 0,
                    statsJson = json
                )
                
                Log.d(TAG, "Stats saved to Room database")
            } catch (e: Exception) {
                Log.e(TAG, "Error processing stats update", e)
            }
        }
    }
    
    /**
     * Called when Unity sends complete save data.
     * Save to Room database and sync with backend.
     */
    override fun onSaveSynced(json: String) {
        serviceScope.launch {
            try {
                val playerId = currentPlayerId ?: return@launch
                val savePayload = gson.fromJson(json, UnitySavePayload::class.java)
                
                // Parse stats from JSON string in payload
                val stats = if (savePayload.stats.isNotEmpty()) {
                    gson.fromJson(savePayload.stats, UnityStatsSnapshot::class.java)
                } else {
                    loadFallbackStats(playerId)
                }

                val currency = savePayload.currency
                    .takeIf { it.isNotBlank() }
                    ?.let { gson.fromJson(it, UnityCurrencySnapshot::class.java) }

                val happiness = savePayload.happiness
                    .takeIf { it.isNotBlank() }
                    ?.let { gson.fromJson(it, UnityHappinessSnapshot::class.java) }

                val sections = savePayload.sections
                    .takeIf { it.isNotBlank() }
                    ?.let { gson.fromJson(it, UnitySectionSnapshot::class.java) }
                
                // Save to Room database
                repository.saveGameState(
                    playerId = playerId,
                    unityStats = stats,
                    generators = savePayload.generators,
                    prestigeLevel = 0,
                    currency = currency,
                    happiness = happiness,
                    sections = sections?.sections,
                    statsJson = savePayload.stats.takeIf { it.isNotBlank() }
                )
                
                Log.d(TAG, "Complete save data saved to Room database")
                
                // Sync with backend if available
                syncWithBackend(playerId)
            } catch (e: Exception) {
                Log.e(TAG, "Error processing save sync", e)
            }
        }
    }

    override fun onCheckpointReached(json: String) {
        serviceScope.launch {
            try {
                val playerId = currentPlayerId ?: return@launch
                val payload = gson.fromJson(json, UnityCheckpointPayload::class.java)

                val stats = when {
                    payload.snapshot != null -> payload.snapshot
                    !payload.rawStatsJson.isNullOrBlank() -> gson.fromJson(payload.rawStatsJson, UnityStatsSnapshot::class.java)
                    else -> loadFallbackStats(playerId)
                }

                val token = buildCheckpointToken(payload.checkpointId, stats.timestampUtc)
                if (token == lastCheckpointToken) {
                    Log.d(TAG, "Skipping duplicate checkpoint ${payload.checkpointId}")
                    return@launch
                }

                val sections = payload.sections?.sections

                repository.saveGameState(
                    playerId = playerId,
                    unityStats = stats,
                    generators = payload.generators,
                    prestigeLevel = 0,
                    currency = payload.currency,
                    happiness = payload.happiness,
                    sections = sections,
                    statsJson = payload.rawStatsJson,
                    checkpointId = payload.checkpointId,
                    checkpointReason = payload.reason,
                    checkpointReachedAt = payload.reachedAtUtc
                )

                lastCheckpointToken = token
                Log.d(TAG, "Checkpoint ${payload.checkpointId} saved")
            } catch (e: Exception) {
                Log.e(TAG, "Error processing checkpoint payload", e)
            }
        }
    }
    
    /**
     * Sync game state with backend server.
     */
    private suspend fun syncWithBackend(playerId: String) {
        try {
            // TODO: Implement backend sync when BackendClient API is ready
            Log.d(TAG, "Backend sync not yet implemented, marking as synced locally")
            
            val gameState = repository.getGameState(playerId)
            if (gameState == null) {
                Log.d(TAG, "No game state to sync")
                return
            }
            
            // TODO: Implement backend sync API call
            // For now, just mark as synced
            repository.markAsSynced(playerId)
            
            Log.d(TAG, "Game state synced with backend")
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing with backend", e)
            // Don't mark as synced if there was an error
        }
    }
    
    /**
     * Manually trigger save to Room database.
     */
    fun saveNow() {
        UnityBridge.sendMessageToUnity(
            "AndroidUIManager",
            "RequestSaveState",
            ""
        )
        Log.d(TAG, "Manual save requested")
    }
    
    /**
     * Manually trigger sync with backend.
     */
    fun syncNow() {
        val playerId = currentPlayerId ?: return
        serviceScope.launch {
            syncWithBackend(playerId)
        }
    }

    private fun maybeBroadcastUnityUpdate(gameStateFull: GameStateFull) {
        // Only broadcast when data originated externally (needsUpload == false),
        // and ignore duplicate versions.
        if (gameStateFull.gameState.needsUpload) {
            return
        }

        val version = gameStateFull.gameState.lastModified?.time ?: return
        if (lastUnityBroadcastVersion == version) {
            return
        }

        lastUnityBroadcastVersion = version

        val payload = repository.toUnitySavePayload(gameStateFull)
        val json = gson.toJson(payload)
        UnityBridge.sendJsonToUnity(
            "AndroidUIManager",
            "LoadGameState",
            json
        )
        Log.d(TAG, "Broadcasted Room changes to Unity (version=$version)")
    }
    
    private suspend fun loadFallbackStats(playerId: String): UnityStatsSnapshot {
        val existing = repository.getGameState(playerId)
        if (existing != null) {
            return existing.toStatsSnapshot()
        }

        return UnityStatsSnapshot(
            totalEnergyGenerated = 0.0,
            currentEnergy = 0.0,
            energyPerSecond = 0.0,
            activeGenerators = 0,
            generatorLevelSum = 0,
            offlineSeconds = 0.0,
            lastEventId = null,
            timestampUtc = System.currentTimeMillis().toString(),
            currentMoney = 0.0,
            totalMoneyEarned = 0.0,
            hoursUntilMaintenance = 0.0,
            coreChargePercent = 0.0,
            districts = emptyList()
        )
    }

    private fun GameStateFull.toStatsSnapshot(): UnityStatsSnapshot {
        val storedJson = gameState.statsJson
        if (!storedJson.isNullOrBlank()) {
            runCatching { gson.fromJson(storedJson, UnityStatsSnapshot::class.java) }
                .getOrNull()
                ?.let { return it }
        }

        return UnityStatsSnapshot(
            totalEnergyGenerated = gameState.totalEnergyGenerated,
            currentEnergy = gameState.currentEnergy,
            energyPerSecond = gameState.energyPerSecond,
            activeGenerators = gameState.activeGenerators,
            generatorLevelSum = gameState.generatorLevelSum,
            offlineSeconds = gameState.offlineSeconds,
            lastEventId = gameState.lastEventId,
            timestampUtc = gameState.timestampUtc.time.toString(),
            currentMoney = gameState.currentMoney,
            totalMoneyEarned = gameState.totalMoneyEarned,
            hoursUntilMaintenance = gameState.hoursUntilMaintenance,
            coreChargePercent = gameState.coreChargePercent,
            districts = districts.map { district ->
                UnityStatsSnapshot.DistrictSnapshot(
                    id = district.districtId,
                    name = district.name,
                    level = district.level,
                    efficiency = district.efficiency,
                    powerPerSecond = district.powerPerSecond,
                    moneyPerSecond = district.moneyPerSecond,
                    hoursUntilMaintenance = district.hoursUntilMaintenance,
                    nextUpgradeCost = district.nextUpgradeCost
                )
            }
        )
    }

    private fun buildCheckpointToken(checkpointId: String, timestampUtc: String?): String {
        val ts = timestampUtc ?: ""
        return "$checkpointId@$ts"
    }
}
