package com.prog7314.IdleLoadsheddingMobile.data.models

import com.google.gson.annotations.SerializedName

data class UnityStatsSnapshot(
    @SerializedName("totalEnergyGenerated") val totalEnergyGenerated: Double,
    @SerializedName("currentEnergy") val currentEnergy: Double,
    @SerializedName("energyPerSecond") val energyPerSecond: Double,
    @SerializedName("activeGenerators") val activeGenerators: Int,
    @SerializedName("generatorLevelSum") val generatorLevelSum: Int,
    @SerializedName("offlineSeconds") val offlineSeconds: Double,
    @SerializedName("lastEventId") val lastEventId: String?,
    @SerializedName("timestampUtc") val timestampUtc: String,
    @SerializedName("currentMoney") val currentMoney: Double = 0.0,
    @SerializedName("totalMoneyEarned") val totalMoneyEarned: Double = 0.0,
    @SerializedName("hoursUntilMaintenance") val hoursUntilMaintenance: Double = 0.0,
    @SerializedName("coreChargePercent") val coreChargePercent: Double = 0.0,
    @SerializedName("districts") val districts: List<DistrictSnapshot>? = emptyList()
) {
    data class DistrictSnapshot(
        @SerializedName("id") val id: String,
        @SerializedName("name") val name: String,
        @SerializedName("level") val level: Int,
        @SerializedName("efficiency") val efficiency: Double,
        @SerializedName("powerPerSecond") val powerPerSecond: Double,
        @SerializedName("moneyPerSecond") val moneyPerSecond: Double,
        @SerializedName("hoursUntilMaintenance") val hoursUntilMaintenance: Double,
        @SerializedName("nextUpgradeCost") val nextUpgradeCost: Double
    )
}

data class BackendStatsSnapshot(
    @SerializedName("playerId") val playerId: String,
    @SerializedName("totalEnergyGenerated") val totalEnergyGenerated: Double,
    @SerializedName("currentEnergy") val currentEnergy: Double,
    @SerializedName("energyPerSecond") val energyPerSecond: Double,
    @SerializedName("activeGenerators") val activeGenerators: Int,
    @SerializedName("generatorLevelSum") val generatorLevelSum: Int,
    @SerializedName("offlineSeconds") val offlineSeconds: Double,
    @SerializedName("lastEventId") val lastEventId: String?,
    @SerializedName("timestampUtc") val timestampUtc: String,
    @SerializedName("currentMoney") val currentMoney: Double = 0.0,
    @SerializedName("totalMoneyEarned") val totalMoneyEarned: Double = 0.0,
    @SerializedName("hoursUntilMaintenance") val hoursUntilMaintenance: Double = 0.0,
    @SerializedName("coreChargePercent") val coreChargePercent: Double = 0.0,
    @SerializedName("districts") val districts: List<UnityStatsSnapshot.DistrictSnapshot>? = emptyList()
)

data class BackendStatsSummary(
    val totalPlayers: Int,
    val averageEnergyPerSecond: Double,
    val topPerformers: List<Performer> = emptyList()
) {
    data class Performer(
        val playerId: String,
        val displayName: String,
        val score: Double
    )
}
