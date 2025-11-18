package com.prog7314.IdleLoadsheddingMobile.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.prog7314.IdleLoadsheddingMobile.data.local.AppDatabase
import com.prog7314.IdleLoadsheddingMobile.data.models.UnityStatsSnapshot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class GameStateRepositoryTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: GameStateRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = GameStateRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun saveGameState_persistsCheckpointMetadata() = runTest {
        val playerId = "player-checkpoint"
        val stats = sampleStats()
        val checkpointId = "tutorial_01"
        val checkpointReason = "Tutorial complete"
        val checkpointTime = 1_728_960_000_000L

        repository.saveGameState(
            playerId = playerId,
            unityStats = stats,
            checkpointId = checkpointId,
            checkpointReason = checkpointReason,
            checkpointReachedAt = checkpointTime
        )

        val stored = repository.getGameState(playerId)
        val entity = requireNotNull(stored?.gameState)

        assertEquals(checkpointId, entity.checkpointId)
        assertEquals(checkpointReason, entity.checkpointReason)
        assertEquals(checkpointTime, entity.checkpointReachedAt?.time)
        assertTrue("Checkpoint saves should mark the record as needing upload", entity.needsUpload)
    }

    private fun sampleStats(): UnityStatsSnapshot {
        return UnityStatsSnapshot(
            totalEnergyGenerated = 100.0,
            currentEnergy = 25.0,
            energyPerSecond = 4.0,
            activeGenerators = 2,
            generatorLevelSum = 4,
            offlineSeconds = 0.0,
            lastEventId = "event-1",
            timestampUtc = System.currentTimeMillis().toString(),
            currentMoney = 50.0,
            totalMoneyEarned = 75.0,
            hoursUntilMaintenance = 3.0,
            coreChargePercent = 12.5,
            districts = emptyList()
        )
    }
}
