package com.prog7314.IdleLoadsheddingMobile.data.local.converters

import androidx.room.TypeConverter
import java.util.Date

/**
 * Room TypeConverter for Date objects.
 * Converts between Date and Long (timestamp) for storage.
 */
class DateConverter {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}
