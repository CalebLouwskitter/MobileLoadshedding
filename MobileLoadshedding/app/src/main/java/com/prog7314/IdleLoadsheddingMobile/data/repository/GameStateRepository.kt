package com.prog7314.IdleLoadsheddingMobile.data.repository

import android.util.Log
import com.google.gson.Gson
import com.prog7314.IdleLoadsheddingMobile.data.local.AppDatabase
import com.prog7314.IdleLoadsheddingMobile.data.local.entities.DistrictEntity
import com.prog7314.IdleLoadsheddingMobile.data.local.entities.GameStateEntity
import com.prog7314.IdleLoadsheddingMobile.data.local.entities.GameStateFull
import com.prog7314.IdleLoadsheddingMobile.data.local.entities.GeneratorEntity
import com.prog7314.IdleLoadsheddingMobile.data.models.*
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.util.Date

/**
 * Repository for game state management.
 * Handles all interactions with local Room database and provides sync capabilities.
 * 
 * Responsibilities:
 * - Save/Load game state locally
 * - Track sync status
 * - Convert between Unity models and database entities
 * - Conflict resolution (last-write-wins strategy)
 */
class GameStateRepository(private val database: AppDatabase) {
    
    private val gameStateDao = database.gameStateDao()
    private val districtDao = database.districtDao()
    private val generatorDao = database.generatorDao()
    private val gson = Gson()
    
    companion object {
        private const val TAG = "GameStateRepository"
    }
    
    /**
     * Get game state as a Flow for reactive updates.
     */
    fun getGameStateFlow(playerId: String): Flow<GameStateFull?> {
        return gameStateDao.getGameStateFlow(playerId)
    }
    
    /**
     * Get current game state (single fetch).
     */
    suspend fun getGameState(playerId: String): GameStateFull? {
        return gameStateDao.getGameState(playerId)
    }
    
    /**
     * Save complete game state from Unity.
     * Marks the state as needing upload to backend.
     */
    suspend fun saveGameState(
        playerId: String,
        unityStats: UnityStatsSnapshot,
        generators: List<UnitySavePayload.GeneratorState>? = null,
        prestigeLevel: Int = 0,
        currency: UnityCurrencySnapshot? = null,
        happiness: UnityHappinessSnapshot? = null,
        sections: List<UnitySectionSnapshot.SectionState>? = null,
        statsJson: String? = null,
        checkpointId: String? = null,
        checkpointReason: String? = null,
        checkpointReachedAt: Long? = null
    ) {
        try {
            val now = Date()
            val existingState = gameStateDao.getGameState(playerId)?.gameState

            val (currentMoney, totalMoneyEarned) = when {
                currency != null -> currency.currentMoney to currency.totalMoneyEarned
                existingState != null -> existingState.currentMoney to existingState.totalMoneyEarned
                else -> unityStats.currentMoney to unityStats.totalMoneyEarned
            }

            val (currentHappiness, totalHappinessPenalty) = when {
                happiness != null -> happiness.currentHappiness to happiness.totalPenalty
                existingState != null -> existingState.currentHappiness to existingState.totalHappinessPenalty
                else -> 0.0 to 0.0
            }

            val resolvedStatsJson = statsJson ?: existingState?.statsJson ?: gson.toJson(unityStats)

            val resolvedCheckpointId = checkpointId ?: existingState?.checkpointId
            val resolvedCheckpointReason = checkpointReason ?: existingState?.checkpointReason
            val resolvedCheckpointReachedAt = when {
                checkpointReachedAt != null -> Date(checkpointReachedAt)
                else -> existingState?.checkpointReachedAt
            }
            
            // Create game state entity
            val gameState = GameStateEntity(
                playerId = playerId,
                totalEnergyGenerated = unityStats.totalEnergyGenerated,
                currentEnergy = unityStats.currentEnergy,
                energyPerSecond = unityStats.energyPerSecond,
                currentMoney = currentMoney,
                totalMoneyEarned = totalMoneyEarned,
                statsJson = resolvedStatsJson,
                currentHappiness = currentHappiness,
                totalHappinessPenalty = totalHappinessPenalty,
                activeGenerators = unityStats.activeGenerators,
                generatorLevelSum = unityStats.generatorLevelSum,
                prestigeLevel = prestigeLevel,
                coreChargePercent = unityStats.coreChargePercent,
                hoursUntilMaintenance = unityStats.hoursUntilMaintenance,
                lastEventId = unityStats.lastEventId,
                offlineSeconds = unityStats.offlineSeconds,
                checkpointId = resolvedCheckpointId,
                checkpointReason = resolvedCheckpointReason,
                checkpointReachedAt = resolvedCheckpointReachedAt,
                timestampUtc = parseTimestamp(unityStats.timestampUtc),
                lastModified = now,
                lastSyncTime = null,
                isSynced = false,
                needsUpload = true
            )
            
            // Save game state
            gameStateDao.insertGameState(gameState)
            
            // Save districts
            val districts = when {
                sections != null -> sections.map { section ->
                    DistrictEntity(
                        playerId = playerId,
                        districtId = section.id,
                        name = section.id,
                        level = section.level,
                        efficiency = if (section.isUnlocked) 1.0 else 0.0,
                        powerPerSecond = 0.0,
                        moneyPerSecond = 0.0,
                        hoursUntilMaintenance = 0.0,
                        nextUpgradeCost = 0.0,
                        lastModified = now
                    )
                }
                !unityStats.districts.isNullOrEmpty() -> unityStats.districts!!.map { district ->
                    DistrictEntity(
                        playerId = playerId,
                        districtId = district.id,
                        name = district.name,
                        level = district.level,
                        efficiency = district.efficiency,
                        powerPerSecond = district.powerPerSecond,
                        moneyPerSecond = district.moneyPerSecond,
                        hoursUntilMaintenance = district.hoursUntilMaintenance,
                        nextUpgradeCost = district.nextUpgradeCost,
                        lastModified = now
                    )
                }
                else -> null
            }
            
            if (districts != null) {
                districtDao.deleteDistricts(playerId)
                if (districts.isNotEmpty()) {
                    districtDao.insertDistricts(districts)
                }
            }
            
            // Save generators
            if (generators != null) {
                val generatorEntities = generators.map { gen ->
                    GeneratorEntity(
                        playerId = playerId,
                        generatorId = gen.id,
                        level = gen.level,
                        lastModified = now
                    )
                }
                generatorDao.deleteGenerators(playerId)
                if (generatorEntities.isNotEmpty()) {
                    generatorDao.insertGenerators(generatorEntities)
                }
            }
            
            Log.d(TAG, "Game state saved for player: $playerId")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving game state", e)
            throw e
        }
    }
    
    /**
     * Mark game state as successfully synced with backend.
     */
    suspend fun markAsSynced(playerId: String) {
        gameStateDao.markAsSynced(playerId, System.currentTimeMillis())
        Log.d(TAG, "Game state marked as synced for player: $playerId")
    }
    
    /**
     * Mark game state as needing upload.
     */
    suspend fun markAsNeedsUpload(playerId: String) {
        gameStateDao.markAsNeedsUpload(playerId)
        Log.d(TAG, "Game state marked as needs upload for player: $playerId")
    }
    
    /**
     * Get all game states that need to be synced to backend.
     */
    suspend fun getUnsyncedStates(): List<GameStateEntity> {
        return gameStateDao.getUnsyncedStates()
    }
    
    /**
     * Delete all game data for a player.
     */
    suspend fun deleteGameState(playerId: String) {
        gameStateDao.deleteGameState(playerId)
        Log.d(TAG, "Game state deleted for player: $playerId")
    }
    
    /**
     * Convert GameStateFull to UnitySavePayload for loading into Unity.
     */
    fun toUnitySavePayload(gameStateFull: GameStateFull): UnitySavePayload {
        val gameState = gameStateFull.gameState
        
        val stats = UnityStatsSnapshot(
            totalEnergyGenerated = gameState.totalEnergyGenerated,
            currentEnergy = gameState.currentEnergy,
            energyPerSecond = gameState.energyPerSecond,
            activeGenerators = gameState.activeGenerators,
            generatorLevelSum = gameState.generatorLevelSum,
            offlineSeconds = gameState.offlineSeconds,
            lastEventId = gameState.lastEventId,
            timestampUtc = gameState.timestampUtc.toString(),
            currentMoney = gameState.currentMoney,
            totalMoneyEarned = gameState.totalMoneyEarned,
            hoursUntilMaintenance = gameState.hoursUntilMaintenance,
            coreChargePercent = gameState.coreChargePercent,
            districts = gameStateFull.districts.map { district ->
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
        val serializedStats = gameState.statsJson ?: gson.toJson(stats)
        
        return UnitySavePayload(
            stats = serializedStats,
            generators = gameStateFull.generators.map { gen ->
                UnitySavePayload.GeneratorState(
                    id = gen.generatorId,
                    level = gen.level
                )
            },
            timestampUtc = gameState.timestampUtc.toString(),
            districts = gameStateFull.districts.map { district ->
                UnitySavePayload.DistrictState(
                    id = district.districtId,
                    level = district.level,
                    efficiency = district.efficiency.toFloat()
                )
            },
            central = UnitySavePayload.CentralState(
                charge = gameState.coreChargePercent
            )
        )
    }
    
    /**
     * Parse ISO timestamp string to Date.
     */
    private fun parseTimestamp(timestamp: String): Date {
        val trimmed = timestamp.trim()
        return try {
            Date.from(Instant.parse(trimmed))
        } catch (_: Exception) {
            trimmed.toLongOrNull()?.let { Date(it) } ?: Date()
        }
    }
}
