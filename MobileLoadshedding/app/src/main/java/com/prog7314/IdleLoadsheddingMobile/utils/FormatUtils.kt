package com.prog7314.IdleLoadsheddingMobile.utils

import android.content.Context
import com.prog7314.IdleLoadsheddingMobile.R
import java.text.DecimalFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * Utility class for formatting numbers, durations, and timestamps.
 * Extracted from MainActivity to centralize formatting logic.
 */
class FormatUtils(private val context: Context) {
    
    private val numberFormatter = DecimalFormat("#,##0.##")
    private val timestampFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
        .withZone(ZoneId.systemDefault())
    
    /**
     * Formats energy values with thousand separators.
     * Energy values can get huge (millions), so we use separators everywhere.
     */
    fun formatEnergy(value: Double): String = numberFormatter.format(value)
    
    /**
     * Formats duration in seconds to human-readable format (hours, minutes, seconds).
     * Progressively scales units: seconds → minutes → hours → days.
     * Shows only the two most significant units to keep strings short.
     */
    fun formatDuration(seconds: Double): String {
        if (seconds <= 0.0) return "0s"
        
        var remaining = seconds.toLong()
        val hours = remaining / 3600
        remaining %= 3600
        val minutes = remaining / 60
        val secs = remaining % 60
        
        // Parts array accumulates non-zero units
        val parts = mutableListOf<String>()
        if (hours > 0) parts.add("${hours}h")
        
        // Show minutes if they exist OR if we already have hours
        if (minutes > 0 || hours > 0) parts.add("${minutes}m")
        
        // Always show seconds if nothing else exists; otherwise only if non-zero
        if (secs > 0 || parts.isEmpty()) parts.add("${secs}s")
        
        return parts.joinToString(" ")
    }
    
    /**
     * Formats UTC ISO timestamp to local time string.
     * Defensive coding: null/blank strings and parse errors fall back to "Unknown".
     */
    fun formatTimestamp(isoString: String?): String {
        if (isoString.isNullOrBlank()) {
            return context.getString(R.string.timestamp_unknown)
        }
        
        return try {
            val instant = Instant.parse(isoString)
            timestampFormatter.format(instant)
        } catch (ex: DateTimeParseException) {
            context.getString(R.string.timestamp_unknown)
        }
    }
}
