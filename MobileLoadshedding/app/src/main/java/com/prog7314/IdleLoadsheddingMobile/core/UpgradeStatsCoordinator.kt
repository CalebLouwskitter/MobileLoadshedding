package com.prog7314.IdleLoadsheddingMobile.core

import android.content.Context
import com.prog7314.IdleLoadsheddingMobile.data.local.AppDatabase
import com.prog7314.IdleLoadsheddingMobile.data.models.UpgradeStats
import com.prog7314.IdleLoadsheddingMobile.data.repository.UpgradeStatsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow

/**
 * Lazily initialized singleton that wires the Room-backed repository to any
 * Android or Unity caller. Initialization happens once per process inside the
 * custom [android.app.Application].
 */
object UpgradeStatsCoordinator {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var repository: UpgradeStatsRepository? = null

    fun init(context: Context) {
        if (repository != null) return
        val db = AppDatabase.getDatabase(context.applicationContext)
        repository = UpgradeStatsRepository(db, applicationScope)
    }

    val statsState: StateFlow<UpgradeStats>
        get() = requireNotNull(repository) { NOT_INITIALIZED }
            .statsState

    suspend fun buyDamageUpgrade(cost: Long, levelDelta: Int = 1) {
        requireNotNull(repository) { NOT_INITIALIZED }
            .buyDamageUpgrade(cost, levelDelta)
    }

    suspend fun grantCurrency(amount: Long) {
        requireNotNull(repository) { NOT_INITIALIZED }
            .grantCurrency(amount)
    }

    suspend fun overwriteFromUnity(damageLevel: Int, currency: Long) {
        requireNotNull(repository) { NOT_INITIALIZED }
            .overwriteFromUnity(damageLevel, currency)
    }

    fun currentDamageLevel(): Int = statsState.value.damageLevel
    fun currentCurrency(): Long = statsState.value.currency

    private const val NOT_INITIALIZED = "UpgradeStatsCoordinator.init(context) must be called first"
}
