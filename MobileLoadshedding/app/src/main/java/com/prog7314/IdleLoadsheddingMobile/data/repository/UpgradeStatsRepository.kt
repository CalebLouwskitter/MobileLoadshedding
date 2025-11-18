package com.prog7314.IdleLoadsheddingMobile.data.repository

import com.prog7314.IdleLoadsheddingMobile.data.local.AppDatabase
import com.prog7314.IdleLoadsheddingMobile.data.local.entities.PlayerUpgradeStatsEntity
import com.prog7314.IdleLoadsheddingMobile.data.models.UpgradeStats
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.room.withTransaction

/**
 * Repository that owns the single source of truth for upgrades/currency. Every
 * write is serialized through Room transactions, and every read is served from
 * a hot [StateFlow] so both Android UI and Unity receive the freshest value.
 */
class UpgradeStatsRepository(
    private val database: AppDatabase,
    private val applicationScope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    private val dao = database.playerUpgradeStatsDao()
    private val defaultEntity = PlayerUpgradeStatsEntity.default()

    val statsState: StateFlow<UpgradeStats> = dao.watchStats()
        .map { (it ?: defaultEntity).toDomain() }
        .stateIn(
            scope = applicationScope,
            started = SharingStarted.Eagerly,
            initialValue = defaultEntity.toDomain()
        )

    init {
        applicationScope.launch(ioDispatcher) {
            if (dao.getStats() == null) {
                dao.upsert(defaultEntity)
            }
        }
    }

    suspend fun buyDamageUpgrade(cost: Long, levelDelta: Int = 1) = withContext(ioDispatcher) {
        require(cost >= 0) { "Cost must be non-negative" }
        database.withTransaction {
            val current = dao.getStats() ?: defaultEntity
            require(current.currency >= cost) { "Insufficient currency" }
            val updated = current.copy(
                damageLevel = current.damageLevel + levelDelta,
                currency = current.currency - cost,
                lastUpdatedEpochMillis = System.currentTimeMillis()
            )
            dao.upsert(updated)
        }
    }

    suspend fun grantCurrency(amount: Long) = withContext(ioDispatcher) {
        if (amount == 0L) return@withContext
        database.withTransaction {
            val current = dao.getStats() ?: defaultEntity
            val updated = current.copy(
                currency = (current.currency + amount).coerceAtLeast(0),
                lastUpdatedEpochMillis = System.currentTimeMillis()
            )
            dao.upsert(updated)
        }
    }

    suspend fun overwriteFromUnity(damageLevel: Int, currency: Long) = withContext(ioDispatcher) {
        database.withTransaction {
            val current = dao.getStats() ?: defaultEntity
            val updated = current.copy(
                damageLevel = damageLevel,
                currency = currency,
                lastUpdatedEpochMillis = System.currentTimeMillis()
            )
            dao.upsert(updated)
        }
    }

    private fun PlayerUpgradeStatsEntity.toDomain(): UpgradeStats =
        UpgradeStats(
            damageLevel = damageLevel,
            currency = currency,
            lastUpdatedEpochMillis = lastUpdatedEpochMillis
        )
}
