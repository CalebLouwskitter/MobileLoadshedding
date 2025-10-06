package com.prog7314.IdleLoadsheddingMobile.data.models

data class FriendsPayload(
    val friends: List<FriendProfile>,
    val timestampUtc: String
)

data class FriendProfile(
    val id: String,
    val displayName: String,
    val isOnline: Boolean,
    val contributionPerHour: Double,
    val lastInteractionUtc: String
)

data class FriendSubmission(
    val id: String,
    val displayName: String,
    val contributionPerHour: Double,
    val isOnline: Boolean,
    val remove: Boolean
)

data class FriendResponseDto(
    @com.google.gson.annotations.SerializedName("userId") val userId: Int,
    @com.google.gson.annotations.SerializedName("username") val username: String,
    @com.google.gson.annotations.SerializedName("addedAt") val addedAt: String
)

data class FriendsSyncRequest(
    val playerId: String,
    val friends: List<FriendProfile>
)
