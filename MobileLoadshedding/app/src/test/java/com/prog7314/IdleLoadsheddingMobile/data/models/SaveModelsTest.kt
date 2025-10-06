package com.prog7314.IdleLoadsheddingMobile.data.models

import org.junit.Assert.*
import org.junit.Test

class SaveModelsTest {

    @Test
    fun unitySavePayloadCreationWithAllComponents() {
        val generators = listOf(
            UnitySavePayload.GeneratorState(id = "gen1", level = 5),
            UnitySavePayload.GeneratorState(id = "gen2", level = 10)
        )
        val districts = listOf(
            UnitySavePayload.DistrictState(id = "dist1", level = 3, efficiency = 0.85f),
            UnitySavePayload.DistrictState(id = "dist2", level = 7, efficiency = 0.92f)
        )
        val central = UnitySavePayload.CentralState(charge = 75.5)

        val payload = UnitySavePayload(
            stats = "encoded_stats_data",
            generators = generators,
            timestampUtc = "2025-10-04T10:00:00Z",
            districts = districts,
            central = central
        )

        assertEquals("encoded_stats_data", payload.stats)
        assertEquals(2, payload.generators?.size)
        assertEquals("2025-10-04T10:00:00Z", payload.timestampUtc)
        assertEquals(2, payload.districts?.size)
        assertNotNull(payload.central)
        assertEquals(75.5, payload.central?.charge ?: 0.0, 0.001)
    }

    @Test
    fun unitySavePayloadWithDefaultEmptyLists() {
        val payload = UnitySavePayload(
            stats = "minimal_stats",
            timestampUtc = "2025-10-04T10:00:00Z"
        )

        assertNotNull(payload.generators)
        assertTrue(payload.generators!!.isEmpty())
        assertNotNull(payload.districts)
        assertTrue(payload.districts!!.isEmpty())
        assertNull(payload.central)
    }

    @Test
    fun generatorStateCreation() {
        val generator = UnitySavePayload.GeneratorState(
            id = "generator123",
            level = 15
        )

        assertEquals("generator123", generator.id)
        assertEquals(15, generator.level)
    }

    @Test
    fun generatorStateWithZeroLevel() {
        val generator = UnitySavePayload.GeneratorState(
            id = "newgen",
            level = 0
        )

        assertEquals(0, generator.level)
    }

    @Test
    fun districtStateCreation() {
        val district = UnitySavePayload.DistrictState(
            id = "district456",
            level = 8,
            efficiency = 0.95f
        )

        assertEquals("district456", district.id)
        assertEquals(8, district.level)
        assertEquals(0.95f, district.efficiency, 0.001f)
    }

    @Test
    fun districtStateWithLowEfficiency() {
        val district = UnitySavePayload.DistrictState(
            id = "inefficient",
            level = 1,
            efficiency = 0.1f
        )

        assertEquals(0.1f, district.efficiency, 0.001f)
    }

    @Test
    fun centralStateCreation() {
        val central = UnitySavePayload.CentralState(charge = 100.0)

        assertEquals(100.0, central.charge, 0.001)
    }

    @Test
    fun centralStateWithZeroCharge() {
        val central = UnitySavePayload.CentralState(charge = 0.0)

        assertEquals(0.0, central.charge, 0.001)
    }

    @Test
    fun backendSavePayloadCreation() {
        val generators = listOf(
            BackendGeneratorState(id = "gen1", level = 5),
            BackendGeneratorState(id = "gen2", level = 10)
        )
        val districts = listOf(
            BackendDistrictState(id = "dist1", level = 3, efficiency = 0.85f)
        )
        val central = BackendCentralState(charge = 50.0)

        val payload = BackendSavePayload(
            playerId = "player123",
            stats = "backend_stats",
            generators = generators,
            districts = districts,
            central = central,
            timestampUtc = "2025-10-04T11:00:00Z"
        )

        assertEquals("player123", payload.playerId)
        assertEquals("backend_stats", payload.stats)
        assertEquals(2, payload.generators.size)
        assertEquals(1, payload.districts.size)
        assertNotNull(payload.central)
        assertEquals("2025-10-04T11:00:00Z", payload.timestampUtc)
    }

    @Test
    fun backendSavePayloadWithEmptyDistricts() {
        val generators = listOf(BackendGeneratorState(id = "gen1", level = 1))

        val payload = BackendSavePayload(
            playerId = "player456",
            stats = "stats",
            generators = generators,
            timestampUtc = "2025-10-04T11:00:00Z"
        )

        assertTrue(payload.districts.isEmpty())
        assertNull(payload.central)
    }

    @Test
    fun backendGeneratorStateCreation() {
        val generator = BackendGeneratorState(
            id = "backend_gen",
            level = 20
        )

        assertEquals("backend_gen", generator.id)
        assertEquals(20, generator.level)
    }

    @Test
    fun backendDistrictStateCreation() {
        val district = BackendDistrictState(
            id = "backend_dist",
            level = 12,
            efficiency = 0.88f
        )

        assertEquals("backend_dist", district.id)
        assertEquals(12, district.level)
        assertEquals(0.88f, district.efficiency, 0.001f)
    }

    @Test
    fun backendCentralStateCreation() {
        val central = BackendCentralState(charge = 66.6)

        assertEquals(66.6, central.charge, 0.001)
    }

    @Test
    fun unitySavePayloadWithMultipleGenerators() {
        val generators = (1..10).map { i ->
            UnitySavePayload.GeneratorState(id = "gen$i", level = i)
        }

        val payload = UnitySavePayload(
            stats = "many_generators",
            generators = generators,
            timestampUtc = "2025-10-04T10:00:00Z"
        )

        assertEquals(10, payload.generators?.size)
        assertEquals("gen1", payload.generators?.get(0)?.id)
        assertEquals(10, payload.generators?.get(9)?.level)
    }

    @Test
    fun unitySavePayloadEquality() {
        val payload1 = UnitySavePayload(
            stats = "test",
            timestampUtc = "2025-10-04T10:00:00Z"
        )
        val payload2 = UnitySavePayload(
            stats = "test",
            timestampUtc = "2025-10-04T10:00:00Z"
        )

        assertEquals(payload1, payload2)
        assertEquals(payload1.hashCode(), payload2.hashCode())
    }

    @Test
    fun backendSavePayloadWithNullCentral() {
        val generators = listOf(BackendGeneratorState(id = "gen1", level = 1))
        val payload = BackendSavePayload(
            playerId = "player",
            stats = "stats",
            generators = generators,
            central = null,
            timestampUtc = "2025-10-04T10:00:00Z"
        )

        assertNull(payload.central)
    }
}

