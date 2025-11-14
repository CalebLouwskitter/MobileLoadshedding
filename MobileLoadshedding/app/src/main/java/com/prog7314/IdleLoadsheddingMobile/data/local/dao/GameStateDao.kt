package com.prog7314.IdleLoadsheddingMobile.data.local.dao

import androidx.room.*
import com.prog7314.IdleLoadsheddingMobile.data.local.entities.GameStateEntity
import com.prog7314.IdleLoadsheddingMobile.data.local.entities.GameStateFull
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for GameState operations.
 * Provides methods to read/write game state to local database.
 */
@Dao
interface GameStateDao {
    
    /**
     * Get the current game state for a player as a Flow.
     * Flow automatically updates when data changes.
     */
    @Transaction
    @Query("SELECT * FROM game_states WHERE playerId = :playerId")
    fun getGameStateFlow(playerId: String): Flow<GameStateFull?>
    
    /**
     * Get the current game state for a player (single fetch).
     */
    @Transaction
    @Query("SELECT * FROM game_states WHERE playerId = :playerId")
    suspend fun getGameState(playerId: String): GameStateFull?
    
    /**
     * Get all game states (for multi-account support).
     */
    @Transaction
    @Query("SELECT * FROM game_states")
    suspend fun getAllGameStates(): List<GameStateFull>
    
    /**
     * Insert or update game state.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGameState(gameState: GameStateEntity)
    
    /**
     * Update game state.
     */
    @Update
    suspend fun updateGameState(gameState: GameStateEntity)
    
    /**
     * Delete game state for a player.
     */
    @Query("DELETE FROM game_states WHERE playerId = :playerId")
    suspend fun deleteGameState(playerId: String)
    
    /**
     * Get all game states that need to be synced to backend.
     */
    @Query("SELECT * FROM game_states WHERE needsUpload = 1")
    suspend fun getUnsyncedStates(): List<GameStateEntity>
    
    /**
     * Mark game state as synced.
     */
    @Query("UPDATE game_states SET isSynced = 1, needsUpload = 0, lastSyncTime = :syncTime WHERE playerId = :playerId")
    suspend fun markAsSynced(playerId: String, syncTime: Long)
    
    /**
     * Mark game state as needing upload.
     */
    @Query("UPDATE game_states SET needsUpload = 1 WHERE playerId = :playerId")
    suspend fun markAsNeedsUpload(playerId: String)
}
