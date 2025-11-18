package com.prog7314.IdleLoadsheddingMobile.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.prog7314.IdleLoadsheddingMobile.data.local.entities.PlayerUpgradeStatsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerUpgradeStatsDao {

    @Query("SELECT * FROM player_upgrade_stats WHERE id = :rowId")
    fun watchStats(rowId: Long = PlayerUpgradeStatsEntity.DEFAULT_ROW_ID): Flow<PlayerUpgradeStatsEntity?>

    @Query("SELECT * FROM player_upgrade_stats WHERE id = :rowId")
    suspend fun getStats(rowId: Long = PlayerUpgradeStatsEntity.DEFAULT_ROW_ID): PlayerUpgradeStatsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PlayerUpgradeStatsEntity)

    @Query("UPDATE player_upgrade_stats SET currency = currency + :delta, lastUpdatedEpochMillis = :timestamp WHERE id = :rowId")
    suspend fun incrementCurrency(delta: Long, timestamp: Long, rowId: Long = PlayerUpgradeStatsEntity.DEFAULT_ROW_ID)
}
