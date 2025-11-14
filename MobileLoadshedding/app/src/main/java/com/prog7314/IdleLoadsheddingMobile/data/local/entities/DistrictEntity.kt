package com.prog7314.IdleLoadsheddingMobile.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.prog7314.IdleLoadsheddingMobile.data.local.converters.DateConverter
import java.util.Date

/**
 * Room entity representing individual district/generator states.
 * Linked to GameStateEntity via foreign key.
 */
@Entity(
    tableName = "districts",
    foreignKeys = [
        ForeignKey(
            entity = GameStateEntity::class,
            parentColumns = ["playerId"],
            childColumns = ["playerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["playerId"]), Index(value = ["districtId"])]
)
@TypeConverters(DateConverter::class)
data class DistrictEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val playerId: String,
    val districtId: String,
    val name: String,
    val level: Int,
    val efficiency: Double,
    val powerPerSecond: Double,
    val moneyPerSecond: Double,
    val hoursUntilMaintenance: Double,
    val nextUpgradeCost: Double,
    
    val lastModified: Date
)
