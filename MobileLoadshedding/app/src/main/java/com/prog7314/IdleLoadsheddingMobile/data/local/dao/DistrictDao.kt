package com.prog7314.IdleLoadsheddingMobile.data.local.dao

import androidx.room.*
import com.prog7314.IdleLoadsheddingMobile.data.local.entities.DistrictEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for District operations.
 */
@Dao
interface DistrictDao {
    
    /**
     * Get all districts for a player as a Flow.
     */
    @Query("SELECT * FROM districts WHERE playerId = :playerId")
    fun getDistrictsFlow(playerId: String): Flow<List<DistrictEntity>>
    
    /**
     * Get all districts for a player (single fetch).
     */
    @Query("SELECT * FROM districts WHERE playerId = :playerId")
    suspend fun getDistricts(playerId: String): List<DistrictEntity>
    
    /**
     * Get a specific district.
     */
    @Query("SELECT * FROM districts WHERE playerId = :playerId AND districtId = :districtId")
    suspend fun getDistrict(playerId: String, districtId: String): DistrictEntity?
    
    /**
     * Insert or update districts.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDistricts(districts: List<DistrictEntity>)
    
    /**
     * Insert or update a single district.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDistrict(district: DistrictEntity)
    
    /**
     * Update district.
     */
    @Update
    suspend fun updateDistrict(district: DistrictEntity)
    
    /**
     * Delete all districts for a player.
     */
    @Query("DELETE FROM districts WHERE playerId = :playerId")
    suspend fun deleteDistricts(playerId: String)
    
    /**
     * Delete a specific district.
     */
    @Query("DELETE FROM districts WHERE playerId = :playerId AND districtId = :districtId")
    suspend fun deleteDistrict(playerId: String, districtId: String)
}
