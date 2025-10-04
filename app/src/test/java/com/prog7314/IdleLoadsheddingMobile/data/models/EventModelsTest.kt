package com.prog7314.IdleLoadsheddingMobile.data.models

import org.junit.Assert.*
import org.junit.Test

class EventModelsTest {

    @Test
    fun unityEventPayloadCreationWithValidData() {
        val event = UnityEventPayload(
            id = "event123",
            title = "Power Surge",
            description = "A sudden increase in power generation",
            isStarting = true,
            durationSeconds = 300f,
            productionMultiplier = 2.5f,
            timestampUtc = "2025-10-04T10:00:00Z"
        )

        assertEquals("event123", event.id)
        assertEquals("Power Surge", event.title)
        assertEquals("A sudden increase in power generation", event.description)
        assertTrue(event.isStarting)
        assertEquals(300f, event.durationSeconds, 0.001f)
        assertEquals(2.5f, event.productionMultiplier, 0.001f)
        assertEquals("2025-10-04T10:00:00Z", event.timestampUtc)
    }

    @Test
    fun unityEventPayloadForEventStarting() {
        val event = UnityEventPayload(
            id = "start_event",
            title = "Event Starting",
            description = "Test event",
            isStarting = true,
            durationSeconds = 600f,
            productionMultiplier = 1.5f,
            timestampUtc = "2025-10-04T10:00:00Z"
        )

        assertTrue(event.isStarting)
        assertEquals(600f, event.durationSeconds, 0.001f)
    }

    @Test
    fun unityEventPayloadForEventEnding() {
        val event = UnityEventPayload(
            id = "end_event",
            title = "Event Ending",
            description = "Test event ending",
            isStarting = false,
            durationSeconds = 0f,
            productionMultiplier = 1.0f,
            timestampUtc = "2025-10-04T11:00:00Z"
        )

        assertFalse(event.isStarting)
        assertEquals(0f, event.durationSeconds, 0.001f)
        assertEquals(1.0f, event.productionMultiplier, 0.001f)
    }

    @Test
    fun unityEventPayloadWithHighMultiplier() {
        val event = UnityEventPayload(
            id = "mega_event",
            title = "Mega Boost",
            description = "Massive production boost",
            isStarting = true,
            durationSeconds = 120f,
            productionMultiplier = 10.0f,
            timestampUtc = "2025-10-04T10:00:00Z"
        )

        assertEquals(10.0f, event.productionMultiplier, 0.001f)
    }

    @Test
    fun unityEventPayloadWithLowMultiplier() {
        val event = UnityEventPayload(
            id = "penalty_event",
            title = "System Malfunction",
            description = "Production reduced",
            isStarting = true,
            durationSeconds = 180f,
            productionMultiplier = 0.5f,
            timestampUtc = "2025-10-04T10:00:00Z"
        )

        assertEquals(0.5f, event.productionMultiplier, 0.001f)
        assertTrue(event.productionMultiplier < 1.0f)
    }

    @Test
    fun unityEventPayloadWithLongDuration() {
        val event = UnityEventPayload(
            id = "long_event",
            title = "Extended Event",
            description = "Long lasting event",
            isStarting = true,
            durationSeconds = 3600f,
            productionMultiplier = 1.2f,
            timestampUtc = "2025-10-04T10:00:00Z"
        )

        assertEquals(3600f, event.durationSeconds, 0.001f)
    }

    @Test
    fun unityEventPayloadWithShortDuration() {
        val event = UnityEventPayload(
            id = "quick_event",
            title = "Quick Boost",
            description = "Short boost",
            isStarting = true,
            durationSeconds = 30f,
            productionMultiplier = 3.0f,
            timestampUtc = "2025-10-04T10:00:00Z"
        )

        assertEquals(30f, event.durationSeconds, 0.001f)
    }

    @Test
    fun unityEventPayloadWithEmptyDescription() {
        val event = UnityEventPayload(
            id = "no_desc_event",
            title = "No Description",
            description = "",
            isStarting = true,
            durationSeconds = 100f,
            productionMultiplier = 1.0f,
            timestampUtc = "2025-10-04T10:00:00Z"
        )

        assertTrue(event.description.isEmpty())
    }

    @Test
    fun unityEventPayloadEqualityCheck() {
        val event1 = UnityEventPayload(
            id = "event1",
            title = "Test Event",
            description = "Test",
            isStarting = true,
            durationSeconds = 100f,
            productionMultiplier = 1.5f,
            timestampUtc = "2025-10-04T10:00:00Z"
        )
        val event2 = UnityEventPayload(
            id = "event1",
            title = "Test Event",
            description = "Test",
            isStarting = true,
            durationSeconds = 100f,
            productionMultiplier = 1.5f,
            timestampUtc = "2025-10-04T10:00:00Z"
        )

        assertEquals(event1, event2)
        assertEquals(event1.hashCode(), event2.hashCode())
    }

    @Test
    fun unityEventPayloadCopyFunction() {
        val original = UnityEventPayload(
            id = "original",
            title = "Original Title",
            description = "Original",
            isStarting = true,
            durationSeconds = 200f,
            productionMultiplier = 2.0f,
            timestampUtc = "2025-10-04T10:00:00Z"
        )
        val modified = original.copy(title = "Modified Title", productionMultiplier = 3.0f)

        assertEquals("original", modified.id)
        assertEquals("Modified Title", modified.title)
        assertEquals(3.0f, modified.productionMultiplier, 0.001f)
        assertEquals(200f, modified.durationSeconds, 0.001f)
    }

    @Test
    fun unityEventPayloadDifferentEventsAreNotEqual() {
        val event1 = UnityEventPayload(
            id = "event1",
            title = "Event 1",
            description = "First",
            isStarting = true,
            durationSeconds = 100f,
            productionMultiplier = 1.5f,
            timestampUtc = "2025-10-04T10:00:00Z"
        )
        val event2 = UnityEventPayload(
            id = "event2",
            title = "Event 2",
            description = "Second",
            isStarting = true,
            durationSeconds = 100f,
            productionMultiplier = 1.5f,
            timestampUtc = "2025-10-04T10:00:00Z"
        )

        assertNotEquals(event1, event2)
    }
}
