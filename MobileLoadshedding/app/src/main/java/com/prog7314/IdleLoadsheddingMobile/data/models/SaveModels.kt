package com.prog7314.IdleLoadsheddingMobile.data.models

data class UnitySavePayload(
    val stats: String,
    val currency: String = "",
    val sections: String = "",
    val happiness: String = "",
    val generators: List<GeneratorState>? = emptyList(),
    val timestampUtc: String,
    val districts: List<DistrictState>? = emptyList(),
    val central: CentralState? = null
) {
    data class GeneratorState(
        val id: String,
        val level: Int
    )

    data class DistrictState(
        val id: String,
        val level: Int,
        val efficiency: Float
    )

    data class CentralState(
        val charge: Double
    )
}

data class UnityCurrencySnapshot(
    val currentMoney: Double = 0.0,
    val totalMoneyEarned: Double = 0.0,
    val timestampUtc: String = ""
)

data class UnityHappinessSnapshot(
    val currentHappiness: Double = 0.0,
    val totalPenalty: Double = 0.0,
    val timestampUtc: String = ""
)

data class UnitySectionSnapshot(
    val sections: List<SectionState> = emptyList(),
    val timestampUtc: String = ""
) {
    data class SectionState(
        val id: String,
        val level: Int,
        val isUnlocked: Boolean
    )
}

data class BackendSavePayload(
    val playerId: String,
    val stats: String,
    val generators: List<BackendGeneratorState>,
    val districts: List<BackendDistrictState> = emptyList(),
    val central: BackendCentralState? = null,
    val timestampUtc: String
)

data class BackendGeneratorState(
    val id: String,
    val level: Int
)

data class BackendDistrictState(
    val id: String,
    val level: Int,
    val efficiency: Float
)

data class BackendCentralState(
    val charge: Double
)

data class UnityCheckpointPayload(
    val checkpointId: String,
    val playerId: String? = null,
    val reason: String? = null,
    val reachedAtUtc: Long? = null,
    val snapshot: UnityStatsSnapshot? = null,
    val rawStatsJson: String? = null,
    val currency: UnityCurrencySnapshot? = null,
    val happiness: UnityHappinessSnapshot? = null,
    val sections: UnitySectionSnapshot? = null,
    val generators: List<UnitySavePayload.GeneratorState>? = null
)
