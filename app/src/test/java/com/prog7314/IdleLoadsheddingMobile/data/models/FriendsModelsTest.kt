package com.prog7314.IdleLoadsheddingMobile.data.models

import org.junit.Assert.*
import org.junit.Test

class FriendsModelsTest {

    @Test
    fun friendsPayloadCreationWithValidData() {
        val friends = listOf(
            FriendProfile(
                id = "friend1",
                displayName = "John Doe",
                isOnline = true,
                contributionPerHour = 100.5,
                lastInteractionUtc = "2025-10-04T10:00:00Z"
            )
        )
        val payload = FriendsPayload(
            friends = friends,
            timestampUtc = "2025-10-04T10:00:00Z"
        )

        assertEquals(1, payload.friends.size)
        assertEquals("2025-10-04T10:00:00Z", payload.timestampUtc)
    }

    @Test
    fun friendsPayloadWithEmptyFriendsList() {
        val payload = FriendsPayload(
            friends = emptyList(),
            timestampUtc = "2025-10-04T10:00:00Z"
        )

        assertTrue(payload.friends.isEmpty())
        assertEquals("2025-10-04T10:00:00Z", payload.timestampUtc)
    }

    @Test
    fun friendProfileCreationWithValidData() {
        val profile = FriendProfile(
            id = "friend123",
            displayName = "Jane Smith",
            isOnline = false,
            contributionPerHour = 250.75,
            lastInteractionUtc = "2025-10-03T15:30:00Z"
        )

        assertEquals("friend123", profile.id)
        assertEquals("Jane Smith", profile.displayName)
        assertFalse(profile.isOnline)
        assertEquals(250.75, profile.contributionPerHour, 0.001)
        assertEquals("2025-10-03T15:30:00Z", profile.lastInteractionUtc)
    }

    @Test
    fun friendProfileWithZeroContribution() {
        val profile = FriendProfile(
            id = "friend456",
            displayName = "New User",
            isOnline = true,
            contributionPerHour = 0.0,
            lastInteractionUtc = "2025-10-04T10:00:00Z"
        )

        assertEquals(0.0, profile.contributionPerHour, 0.001)
    }

    @Test
    fun friendSubmissionForAddingFriend() {
        val submission = FriendSubmission(
            id = "friend789",
            displayName = "Bob Johnson",
            contributionPerHour = 150.0,
            isOnline = true,
            remove = false
        )

        assertEquals("friend789", submission.id)
        assertEquals("Bob Johnson", submission.displayName)
        assertEquals(150.0, submission.contributionPerHour, 0.001)
        assertTrue(submission.isOnline)
        assertFalse(submission.remove)
    }

    @Test
    fun friendSubmissionForRemovingFriend() {
        val submission = FriendSubmission(
            id = "friend999",
            displayName = "Removed User",
            contributionPerHour = 0.0,
            isOnline = false,
            remove = true
        )

        assertTrue(submission.remove)
    }

    @Test
    fun friendResponseDtoWithValidBackendData() {
        val response = FriendResponseDto(
            userId = 12345,
            username = "testuser",
            addedAt = "2025-10-04T10:00:00Z"
        )

        assertEquals(12345, response.userId)
        assertEquals("testuser", response.username)
        assertEquals("2025-10-04T10:00:00Z", response.addedAt)
    }

    @Test
    fun friendProfileEqualityCheck() {
        val profile1 = FriendProfile(
            id = "friend1",
            displayName = "Test",
            isOnline = true,
            contributionPerHour = 100.0,
            lastInteractionUtc = "2025-10-04T10:00:00Z"
        )
        val profile2 = FriendProfile(
            id = "friend1",
            displayName = "Test",
            isOnline = true,
            contributionPerHour = 100.0,
            lastInteractionUtc = "2025-10-04T10:00:00Z"
        )

        assertEquals(profile1, profile2)
        assertEquals(profile1.hashCode(), profile2.hashCode())
    }

    @Test
    fun friendProfileCopyFunction() {
        val original = FriendProfile(
            id = "friend1",
            displayName = "Original",
            isOnline = true,
            contributionPerHour = 100.0,
            lastInteractionUtc = "2025-10-04T10:00:00Z"
        )
        val modified = original.copy(displayName = "Modified")

        assertEquals("friend1", modified.id)
        assertEquals("Modified", modified.displayName)
        assertEquals(original.contributionPerHour, modified.contributionPerHour, 0.001)
    }
}
