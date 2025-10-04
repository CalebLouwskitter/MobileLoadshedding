package com.prog7314.IdleLoadsheddingMobile.data.models

import org.junit.Assert.*
import org.junit.Test

class HealthResponseTest {

    @Test
    fun healthResponseCreationWithValidData() {
        val response = HealthResponse(
            status = "healthy",
            utc = "2025-10-04T10:00:00Z"
        )

        assertEquals("healthy", response.status)
        assertEquals("2025-10-04T10:00:00Z", response.utc)
    }

    @Test
    fun healthResponseWithNullUtc() {
        val response = HealthResponse(
            status = "healthy",
            utc = null
        )

        assertEquals("healthy", response.status)
        assertNull(response.utc)
    }

    @Test
    fun healthResponseWithOkStatus() {
        val response = HealthResponse(
            status = "ok",
            utc = "2025-10-04T11:00:00Z"
        )

        assertEquals("ok", response.status)
        assertNotNull(response.utc)
    }

    @Test
    fun healthResponseWithErrorStatus() {
        val response = HealthResponse(
            status = "error",
            utc = "2025-10-04T12:00:00Z"
        )

        assertEquals("error", response.status)
    }

    @Test
    fun healthResponseWithDegradedStatus() {
        val response = HealthResponse(
            status = "degraded",
            utc = "2025-10-04T13:00:00Z"
        )

        assertEquals("degraded", response.status)
    }

    @Test
    fun healthResponseWithEmptyStatus() {
        val response = HealthResponse(
            status = "",
            utc = "2025-10-04T10:00:00Z"
        )

        assertTrue(response.status.isEmpty())
    }

    @Test
    fun healthResponseEqualityCheck() {
        val response1 = HealthResponse(
            status = "healthy",
            utc = "2025-10-04T10:00:00Z"
        )
        val response2 = HealthResponse(
            status = "healthy",
            utc = "2025-10-04T10:00:00Z"
        )

        assertEquals(response1, response2)
        assertEquals(response1.hashCode(), response2.hashCode())
    }

    @Test
    fun healthResponseDifferentStatusesAreNotEqual() {
        val response1 = HealthResponse(
            status = "healthy",
            utc = "2025-10-04T10:00:00Z"
        )
        val response2 = HealthResponse(
            status = "error",
            utc = "2025-10-04T10:00:00Z"
        )

        assertNotEquals(response1, response2)
    }

    @Test
    fun healthResponseCopyFunction() {
        val original = HealthResponse(
            status = "healthy",
            utc = "2025-10-04T10:00:00Z"
        )
        val modified = original.copy(status = "degraded")

        assertEquals("degraded", modified.status)
        assertEquals("2025-10-04T10:00:00Z", modified.utc)
    }

    @Test
    fun healthResponseWithDifferentUtcTimes() {
        val response1 = HealthResponse(
            status = "healthy",
            utc = "2025-10-04T10:00:00Z"
        )
        val response2 = HealthResponse(
            status = "healthy",
            utc = "2025-10-04T11:00:00Z"
        )

        assertNotEquals(response1, response2)
    }

    @Test
    fun healthResponseToStringContainsStatus() {
        val response = HealthResponse(
            status = "healthy",
            utc = "2025-10-04T10:00:00Z"
        )

        val string = response.toString()
        assertTrue(string.contains("healthy"))
    }

    @Test
    fun healthResponseWithNullUtcEquality() {
        val response1 = HealthResponse(
            status = "healthy",
            utc = null
        )
        val response2 = HealthResponse(
            status = "healthy",
            utc = null
        )

        assertEquals(response1, response2)
    }
}
