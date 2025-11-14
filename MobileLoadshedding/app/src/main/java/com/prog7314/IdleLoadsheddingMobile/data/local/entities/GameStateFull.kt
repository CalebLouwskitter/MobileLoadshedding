package com.prog7314.IdleLoadsheddingMobile.data.local.entities

import androidx.room.Embedded
import androidx.room.Relation

/**
 * Room relation representing complete game save with all related data.
 * Used to load the full game state in a single query.
 */
data class GameStateFull(
    @Embedded
    val gameState: GameStateEntity,
    
    @Relation(
        parentColumn = "playerId",
        entityColumn = "playerId"
    )
    val districts: List<DistrictEntity>,
    
    @Relation(
        parentColumn = "playerId",
        entityColumn = "playerId"
    )
    val generators: List<GeneratorEntity>
)
