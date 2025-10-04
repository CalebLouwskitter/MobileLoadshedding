package com.prog7314.IdleLoadsheddingMobile.data.models

import org.junit.Assert.*
import org.junit.Test

class LeaderboardModelsTest {

    @Test
    fun unityLeaderboardPayloadCreationWithEntries() {
        val entries = listOf(
            UnityLeaderboardPayload.Entry(
                playerId = "player1",
                displayName = "TopPlayer",
                score = 10000.0,
                lastUpdatedUtc = "2025-10-04T10:00:00Z"
            ),
            UnityLeaderboardPayload.Entry(
                playerId = "player2",
                displayName = "SecondPlayer",
                score = 9000.0,
                lastUpdatedUtc = "2025-10-04T09:00:00Z"
            )
        )
        val payload = UnityLeaderboardPayload(
            entries = entries,
            timestampUtc = "2025-10-04T10:00:00Z"
        )

        assertEquals(2, payload.entries.size)
        assertEquals("2025-10-04T10:00:00Z", payload.timestampUtc)
        assertEquals("TopPlayer", payload.entries[0].displayName)
    }

    @Test
    fun unityLeaderboardPayloadWithEmptyEntries() {
        val payload = UnityLeaderboardPayload(
            entries = emptyList(),
            timestampUtc = "2025-10-04T10:00:00Z"
        )

        assertTrue(payload.entries.isEmpty())
    }

    @Test
    fun unityLeaderboardPayloadEntryWithValidData() {
        val entry = UnityLeaderboardPayload.Entry(
            playerId = "player123",
            displayName = "ProGamer",
            score = 50000.5,
            lastUpdatedUtc = "2025-10-04T12:30:00Z"
        )

        assertEquals("player123", entry.playerId)
        assertEquals("ProGamer", entry.displayName)
        assertEquals(50000.5, entry.score, 0.001)
        assertEquals("2025-10-04T12:30:00Z", entry.lastUpdatedUtc)
    }

    @Test
    fun unityLeaderboardPayloadEntryWithZeroScore() {
        val entry = UnityLeaderboardPayload.Entry(
            playerId = "newplayer",
            displayName = "Beginner",
            score = 0.0,
            lastUpdatedUtc = "2025-10-04T10:00:00Z"
        )

        assertEquals(0.0, entry.score, 0.001)
    }

    @Test
    fun leaderboardSubmissionCreation() {
        val submission = LeaderboardSubmission(
            playerId = "player456",
            displayName = "SubmittingPlayer",
            score = 7500.25
        )

        assertEquals("player456", submission.playerId)
        assertEquals("SubmittingPlayer", submission.displayName)
        assertEquals(7500.25, submission.score, 0.001)
    }

    @Test
    fun leaderboardSubmissionWithNegativeScore() {
        val submission = LeaderboardSubmission(
            playerId = "player789",
            displayName = "TestPlayer",
            score = -100.0
        )

        assertEquals(-100.0, submission.score, 0.001)
    }

    @Test
    fun leaderboardEntryResponseFromBackend() {
        val response = LeaderboardEntryResponse(
            username = "champion",
            score = 99999.99,
            rank = 1,
            recordedAt = "2025-10-04T15:45:00Z"
        )

        assertEquals("champion", response.username)
        assertEquals(99999.99, response.score, 0.001)
        assertEquals(1, response.rank)
        assertEquals("2025-10-04T15:45:00Z", response.recordedAt)
    }

    @Test
    fun leaderboardEntryResponseWithDifferentRanks() {
        val response1 = LeaderboardEntryResponse(
            username = "first",
            score = 1000.0,
            rank = 1,
            recordedAt = "2025-10-04T10:00:00Z"
        )
        val response2 = LeaderboardEntryResponse(
            username = "second",
            score = 900.0,
            rank = 2,
            recordedAt = "2025-10-04T10:00:00Z"
        )

        assertTrue(response1.rank < response2.rank)
        assertTrue(response1.score > response2.score)
    }

    @Test
    fun unityLeaderboardPayloadEntryEquality() {
        val entry1 = UnityLeaderboardPayload.Entry(
            playerId = "player1",
            displayName = "Test",
            score = 100.0,
            lastUpdatedUtc = "2025-10-04T10:00:00Z"
        )
        val entry2 = UnityLeaderboardPayload.Entry(
            playerId = "player1",
            displayName = "Test",
            score = 100.0,
            lastUpdatedUtc = "2025-10-04T10:00:00Z"
        )

        assertEquals(entry1, entry2)
        assertEquals(entry1.hashCode(), entry2.hashCode())
    }

    @Test
    fun leaderboardSubmissionCopyFunction() {
        val original = LeaderboardSubmission(
            playerId = "player1",
            displayName = "Original",
            score = 100.0
        )
        val modified = original.copy(score = 200.0)

        assertEquals("player1", modified.playerId)
        assertEquals("Original", modified.displayName)
        assertEquals(200.0, modified.score, 0.001)
    }
}
