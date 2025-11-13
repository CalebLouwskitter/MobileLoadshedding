package com.prog7314.IdleLoadsheddingMobile.data.models

/**
 * Request model for game state synchronization.
 * Sent to backend to validate player state and calculate offline earnings.
 */
data class GameSyncRequest(
    val moneyEarned: Double,
    val electricityGenerated: Double,
    val prestigeLevel: Int
)

/**
 * Response model from game sync endpoint.
 * Contains validated server-side game state and calculated offline earnings.
 */
data class GameSyncResponse(
    val totalMoney: Double,
    val offlineEarnings: Double,
    val electricityRate: Double,
    val prestigeLevel: Int
)

/**
 * Request model for upgrade purchase validation.
 * Sent to backend to validate upgrade purchase.
 */
data class UpgradeRequest(
    val name: String,
    val cost: Double,
    val electricityBoost: Double
)

/**
 * Response model from upgrade endpoint.
 * Contains validated purchase result and updated game state.
 */
data class UpgradeResponse(
    val success: Boolean,
    val newElectricityRate: Double,
    val remainingMoney: Double
)
