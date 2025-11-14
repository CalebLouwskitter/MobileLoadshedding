package com.prog7314.IdleLoadsheddingMobile.data.local.dao

import androidx.room.*
import com.prog7314.IdleLoadsheddingMobile.data.local.entities.GeneratorEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Generator operations.
 */
@Dao
interface GeneratorDao {
    
    /**
     * Get all generators for a player as a Flow.
     */
    @Query("SELECT * FROM generators WHERE playerId = :playerId")
    fun getGeneratorsFlow(playerId: String): Flow<List<GeneratorEntity>>
    
    /**
     * Get all generators for a player (single fetch).
     */
    @Query("SELECT * FROM generators WHERE playerId = :playerId")
    suspend fun getGenerators(playerId: String): List<GeneratorEntity>
    
    /**
     * Get a specific generator.
     */
    @Query("SELECT * FROM generators WHERE playerId = :playerId AND generatorId = :generatorId")
    suspend fun getGenerator(playerId: String, generatorId: String): GeneratorEntity?
    
    /**
     * Insert or update generators.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGenerators(generators: List<GeneratorEntity>)
    
    /**
     * Insert or update a single generator.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGenerator(generator: GeneratorEntity)
    
    /**
     * Update generator.
     */
    @Update
    suspend fun updateGenerator(generator: GeneratorEntity)
    
    /**
     * Delete all generators for a player.
     */
    @Query("DELETE FROM generators WHERE playerId = :playerId")
    suspend fun deleteGenerators(playerId: String)
    
    /**
     * Delete a specific generator.
     */
    @Query("DELETE FROM generators WHERE playerId = :playerId AND generatorId = :generatorId")
    suspend fun deleteGenerator(playerId: String, generatorId: String)
}
