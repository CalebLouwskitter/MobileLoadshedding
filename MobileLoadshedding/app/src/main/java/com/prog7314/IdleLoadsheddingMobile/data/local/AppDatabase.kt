package com.prog7314.IdleLoadsheddingMobile.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.prog7314.IdleLoadsheddingMobile.data.local.converters.DateConverter
import com.prog7314.IdleLoadsheddingMobile.data.local.dao.DistrictDao
import com.prog7314.IdleLoadsheddingMobile.data.local.dao.GameStateDao
import com.prog7314.IdleLoadsheddingMobile.data.local.dao.GeneratorDao
import com.prog7314.IdleLoadsheddingMobile.data.local.dao.PlayerUpgradeStatsDao
import com.prog7314.IdleLoadsheddingMobile.data.local.entities.DistrictEntity
import com.prog7314.IdleLoadsheddingMobile.data.local.entities.GameStateEntity
import com.prog7314.IdleLoadsheddingMobile.data.local.entities.GeneratorEntity
import com.prog7314.IdleLoadsheddingMobile.data.local.entities.PlayerUpgradeStatsEntity

/**
 * Room database for local game state persistence.
 * Enables offline play and synchronization with backend.
 * 
 * Database version: 1
 * 
 * Features:
 * - Local save game storage
 * - Offline capability
 * - Sync status tracking
 * - Multi-account support
 */
@Database(
    entities = [
        GameStateEntity::class,
        DistrictEntity::class,
        GeneratorEntity::class,
        PlayerUpgradeStatsEntity::class
    ],
    version = 5,
    exportSchema = true
)
@TypeConverters(DateConverter::class)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun gameStateDao(): GameStateDao
    abstract fun districtDao(): DistrictDao
    abstract fun generatorDao(): GeneratorDao
    abstract fun playerUpgradeStatsDao(): PlayerUpgradeStatsDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        private const val DATABASE_NAME = "idle_loadshedding_db"
        
        /**
         * Get singleton instance of the database.
         * Thread-safe via double-checked locking.
         */
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration() // For development - remove in production
                    .build()
                
                INSTANCE = instance
                instance
            }
        }
        
        /**
         * Clear the database instance (useful for testing).
         */
        fun clearInstance() {
            INSTANCE = null
        }
    }
}
