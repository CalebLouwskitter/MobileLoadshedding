package com.prog7314.IdleLoadsheddingMobile.data.models

data class UnityLeaderboardPayload(
    val entries: List<Entry>,
    val timestampUtc: String
) {
    data class Entry(
        val playerId: String,
        val displayName: String,
        val score: Double,
        val lastUpdatedUtc: String
    )
}

data class LeaderboardSubmission(
    val playerId: String,
    val displayName: String,
    val score: Double
)

// Backend API response model
data class LeaderboardEntryResponse(
    val username: String,
    val score: Double,
    val rank: Int,
    val recordedAt: String
)
