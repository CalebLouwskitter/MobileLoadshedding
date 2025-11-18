package com.prog7314.IdleLoadsheddingMobile.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.prog7314.IdleLoadsheddingMobile.data.local.converters.DateConverter
import java.util.Date

/**
 * Room entity representing the player's game state.
 * Stored locally for offline play and synchronized with backend.
 */
@Entity(tableName = "game_states")
@TypeConverters(DateConverter::class)
data class GameStateEntity(
    @PrimaryKey
    val playerId: String,
    
    // Core stats
    val totalEnergyGenerated: Double,
    val currentEnergy: Double,
    val energyPerSecond: Double,
    val currentMoney: Double,
    val totalMoneyEarned: Double,
    val statsJson: String?,
    val currentHappiness: Double,
    val totalHappinessPenalty: Double,
    
    // Game progress
    val activeGenerators: Int,
    val generatorLevelSum: Int,
    val prestigeLevel: Int,
    
    // Core state
    val coreChargePercent: Double,
    val hoursUntilMaintenance: Double,
    
    // Events and timing
    val lastEventId: String?,
    val offlineSeconds: Double,
    val checkpointId: String? = null,
    val checkpointReason: String? = null,
    val checkpointReachedAt: Date? = null,
    
    // Timestamps
    val timestampUtc: Date,
    val lastModified: Date,
    
    // Sync metadata
    val lastSyncTime: Date?,
    val isSynced: Boolean = false,
    val syncVersion: Int = 1,
    val needsUpload: Boolean = false
)
