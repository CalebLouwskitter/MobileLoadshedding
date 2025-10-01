package com.prog7314.IdleLoadsheddingMobile.data.models

data class UnitySavePayload(
    val stats: String,
    val generators: List<GeneratorState>,
    val timestampUtc: String,
    val districts: List<DistrictState> = emptyList(),
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
