package com.prog7314.IdleLoadsheddingMobile.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Minimal table that keeps the canonical upgrade + currency snapshot that both
 * Android UI and Unity render against. Row count always equals one (player 0).
 */
@Entity(tableName = "player_upgrade_stats")
data class PlayerUpgradeStatsEntity(
    @PrimaryKey val id: Long = DEFAULT_ROW_ID,
    val damageLevel: Int = DEFAULT_DAMAGE_LEVEL,
    val currency: Long = DEFAULT_CURRENCY,
    val lastUpdatedEpochMillis: Long = System.currentTimeMillis()
) {
    companion object {
        const val DEFAULT_ROW_ID = 0L
        private const val DEFAULT_DAMAGE_LEVEL = 1
        private const val DEFAULT_CURRENCY = 0L

        fun default(): PlayerUpgradeStatsEntity = PlayerUpgradeStatsEntity()
    }
}
