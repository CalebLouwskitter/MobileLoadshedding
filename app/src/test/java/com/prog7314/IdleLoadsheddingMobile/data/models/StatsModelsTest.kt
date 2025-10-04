package com.prog7314.IdleLoadsheddingMobile.data.models

import org.junit.Assert.*
import org.junit.Test

class StatsModelsTest {

    @Test
    fun unityStatsSnapshotCreationWithAllFields() {
        val districts = listOf(
            UnityStatsSnapshot.DistrictSnapshot(
                id = "district1",
                name = "Downtown",
                level = 5,
                efficiency = 0.85,
                powerPerSecond = 100.0,
                moneyPerSecond = 50.0,
                hoursUntilMaintenance = 24.0,
                nextUpgradeCost = 1000.0
            )
        )

        val snapshot = UnityStatsSnapshot(
            totalEnergyGenerated = 50000.0,
            currentEnergy = 1000.0,
            energyPerSecond = 10.5,
            activeGenerators = 5,
            generatorLevelSum = 25,
            offlineSeconds = 3600.0,
            lastEventId = "event123",
            timestampUtc = "2025-10-04T10:00:00Z",
            currentMoney = 5000.0,
            totalMoneyEarned = 25000.0,
            hoursUntilMaintenance = 48.0,
            coreChargePercent = 75.5,
            districts = districts
        )

        assertEquals(50000.0, snapshot.totalEnergyGenerated, 0.001)
        assertEquals(1000.0, snapshot.currentEnergy, 0.001)
        assertEquals(10.5, snapshot.energyPerSecond, 0.001)
        assertEquals(5, snapshot.activeGenerators)
        assertEquals(25, snapshot.generatorLevelSum)
        assertEquals(3600.0, snapshot.offlineSeconds, 0.001)
        assertEquals("event123", snapshot.lastEventId)
        assertEquals("2025-10-04T10:00:00Z", snapshot.timestampUtc)
        assertEquals(5000.0, snapshot.currentMoney, 0.001)
        assertEquals(25000.0, snapshot.totalMoneyEarned, 0.001)
        assertEquals(48.0, snapshot.hoursUntilMaintenance, 0.001)
        assertEquals(75.5, snapshot.coreChargePercent, 0.001)
        assertEquals(1, snapshot.districts?.size)
    }

    @Test
    fun unityStatsSnapshotWithDefaultValues() {
        val snapshot = UnityStatsSnapshot(
            totalEnergyGenerated = 0.0,
            currentEnergy = 0.0,
            energyPerSecond = 0.0,
            activeGenerators = 0,
            generatorLevelSum = 0,
            offlineSeconds = 0.0,
            lastEventId = null,
            timestampUtc = "2025-10-04T10:00:00Z"
        )

        assertEquals(0.0, snapshot.currentMoney, 0.001)
        assertEquals(0.0, snapshot.totalMoneyEarned, 0.001)
        assertEquals(0.0, snapshot.hoursUntilMaintenance, 0.001)
        assertEquals(0.0, snapshot.coreChargePercent, 0.001)
        assertNotNull(snapshot.districts)
        assertTrue(snapshot.districts!!.isEmpty())
        assertNull(snapshot.lastEventId)
    }

    @Test
    fun districtSnapshotCreation() {
        val district = UnityStatsSnapshot.DistrictSnapshot(
            id = "dist001",
            name = "Industrial Zone",
            level = 10,
            efficiency = 0.95,
            powerPerSecond = 500.0,
            moneyPerSecond = 250.0,
            hoursUntilMaintenance = 12.5,
            nextUpgradeCost = 5000.0
        )

        assertEquals("dist001", district.id)
        assertEquals("Industrial Zone", district.name)
        assertEquals(10, district.level)
        assertEquals(0.95, district.efficiency, 0.001)
        assertEquals(500.0, district.powerPerSecond, 0.001)
        assertEquals(250.0, district.moneyPerSecond, 0.001)
        assertEquals(12.5, district.hoursUntilMaintenance, 0.001)
        assertEquals(5000.0, district.nextUpgradeCost, 0.001)
    }

    @Test
    fun backendStatsSnapshotCreationWithPlayerId() {
        val snapshot = BackendStatsSnapshot(
            playerId = "player123",
            totalEnergyGenerated = 100000.0,
            currentEnergy = 2000.0,
            energyPerSecond = 20.0,
            activeGenerators = 10,
            generatorLevelSum = 50,
            offlineSeconds = 7200.0,
            lastEventId = "event456",
            timestampUtc = "2025-10-04T11:00:00Z",
            currentMoney = 10000.0,
            totalMoneyEarned = 50000.0,
            hoursUntilMaintenance = 36.0,
            coreChargePercent = 80.0,
            districts = emptyList()
        )

        assertEquals("player123", snapshot.playerId)
        assertEquals(100000.0, snapshot.totalEnergyGenerated, 0.001)
        assertEquals(20.0, snapshot.energyPerSecond, 0.001)
        assertEquals(10, snapshot.activeGenerators)
        assertTrue(snapshot.districts!!.isEmpty())
    }

    @Test
    fun backendStatsSummaryCreation() {
        val performers = listOf(
            BackendStatsSummary.Performer(
                playerId = "player1",
                displayName = "TopPlayer",
                score = 10000.0
            ),
            BackendStatsSummary.Performer(
                playerId = "player2",
                displayName = "SecondPlayer",
                score = 9000.0
            )
        )

        val summary = BackendStatsSummary(
            totalPlayers = 100,
            averageEnergyPerSecond = 15.5,
            topPerformers = performers
        )

        assertEquals(100, summary.totalPlayers)
        assertEquals(15.5, summary.averageEnergyPerSecond, 0.001)
        assertEquals(2, summary.topPerformers.size)
        assertEquals("TopPlayer", summary.topPerformers[0].displayName)
    }

    @Test
    fun backendStatsSummaryWithEmptyPerformers() {
        val summary = BackendStatsSummary(
            totalPlayers = 50,
            averageEnergyPerSecond = 10.0
        )

        assertTrue(summary.topPerformers.isEmpty())
    }

    @Test
    fun performerCreation() {
        val performer = BackendStatsSummary.Performer(
            playerId = "champion",
            displayName = "Champion Player",
            score = 99999.0
        )

        assertEquals("champion", performer.playerId)
        assertEquals("Champion Player", performer.displayName)
        assertEquals(99999.0, performer.score, 0.001)
    }

    @Test
    fun unityStatsSnapshotWithMultipleDistricts() {
        val districts = listOf(
            UnityStatsSnapshot.DistrictSnapshot(
                id = "d1", name = "District 1", level = 1, efficiency = 0.5,
                powerPerSecond = 10.0, moneyPerSecond = 5.0,
                hoursUntilMaintenance = 24.0, nextUpgradeCost = 100.0
            ),
            UnityStatsSnapshot.DistrictSnapshot(
                id = "d2", name = "District 2", level = 2, efficiency = 0.6,
                powerPerSecond = 20.0, moneyPerSecond = 10.0,
                hoursUntilMaintenance = 20.0, nextUpgradeCost = 200.0
            ),
            UnityStatsSnapshot.DistrictSnapshot(
                id = "d3", name = "District 3", level = 3, efficiency = 0.7,
                powerPerSecond = 30.0, moneyPerSecond = 15.0,
                hoursUntilMaintenance = 18.0, nextUpgradeCost = 300.0
            )
        )

        val snapshot = UnityStatsSnapshot(
            totalEnergyGenerated = 1000.0,
            currentEnergy = 100.0,
            energyPerSecond = 60.0,
            activeGenerators = 3,
            generatorLevelSum = 6,
            offlineSeconds = 0.0,
            lastEventId = null,
            timestampUtc = "2025-10-04T10:00:00Z",
            districts = districts
        )

        assertEquals(3, snapshot.districts?.size)
        assertEquals(60.0, snapshot.energyPerSecond, 0.001)
    }

    @Test
    fun unityStatsSnapshotCopyFunction() {
        val original = UnityStatsSnapshot(
            totalEnergyGenerated = 1000.0,
            currentEnergy = 100.0,
            energyPerSecond = 10.0,
            activeGenerators = 2,
            generatorLevelSum = 5,
            offlineSeconds = 0.0,
            lastEventId = null,
            timestampUtc = "2025-10-04T10:00:00Z"
        )
        val modified = original.copy(currentEnergy = 200.0)

        assertEquals(1000.0, modified.totalEnergyGenerated, 0.001)
        assertEquals(200.0, modified.currentEnergy, 0.001)
    }
}

