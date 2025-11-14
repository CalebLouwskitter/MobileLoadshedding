package com.prog7314.IdleLoadsheddingMobile.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.prog7314.IdleLoadsheddingMobile.data.local.converters.DateConverter
import java.util.Date

/**
 * Room entity representing individual generator states.
 * Linked to GameStateEntity via foreign key.
 */
@Entity(
    tableName = "generators",
    foreignKeys = [
        ForeignKey(
            entity = GameStateEntity::class,
            parentColumns = ["playerId"],
            childColumns = ["playerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["playerId"]), Index(value = ["generatorId"])]
)
@TypeConverters(DateConverter::class)
data class GeneratorEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val playerId: String,
    val generatorId: String,
    val level: Int,
    
    val lastModified: Date
)
