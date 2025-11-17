package com.prog7314.IdleLoadsheddingMobile.utils

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.util.Log
import java.util.Locale

/**
 * Manages locale/language settings for the application.
 * Extracted from MainActivity to separate concerns and reduce code complexity.
 * 
 * Handles:
 * - Language selection and persistence
 * - Locale configuration across Android versions
 * - Activity recreation after language changes
 */
class LocaleManager(private val context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    // Explicit fallback helps us reliably switch back to English even after custom locales were applied.
    private val appDefaultLocale = Locale("en", "ZA")
    
    companion object {
        private const val PREFS_NAME = "app_settings"
        private const val KEY_LANGUAGE_INDEX = "language_index"
        private const val DEFAULT_LANGUAGE_INDEX = 0  // English
        private const val TAG = "LocaleManager"
    }
    
    /**
     * Applies the saved locale preference on activity startup.
     * MUST be called in onCreate() BEFORE view inflation.
     */
    fun applySavedLocale() {
        val savedIndex = prefs.getInt(KEY_LANGUAGE_INDEX, DEFAULT_LANGUAGE_INDEX)
        
        // Get language codes array - need to use context.resources
        val languageCodes = try {
            context.resources.getStringArray(com.prog7314.IdleLoadsheddingMobile.R.array.language_codes)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load language codes array", e)
            return
        }
        
        // Validate array bounds to prevent IndexOutOfBoundsException
        if (savedIndex in languageCodes.indices) {
            applyLocale(savedIndex)
        } else {
            // Corrupted preference or removed language—reset to default
            Log.w(TAG, "Invalid language index $savedIndex, resetting to default")
            prefs.edit().putInt(KEY_LANGUAGE_INDEX, DEFAULT_LANGUAGE_INDEX).apply()
        }
    }
    
    /**
     * Updates the app's locale configuration based on the given language index.
     * 
     * @param languageIndex Index into the settings_languages and language_codes arrays
     */
    fun applyLocale(languageIndex: Int) {
        try {
            val localeCodes = context.resources.getStringArray(
                com.prog7314.IdleLoadsheddingMobile.R.array.language_codes
            )
            
            // Bounds check again (paranoid but prevents crashes in production)
            if (languageIndex !in localeCodes.indices) {
                Log.e(TAG, "Language index $languageIndex out of bounds")
                return
            }
            
            val localeCode = localeCodes[languageIndex]
            
            val locale = when {
                localeCode.isBlank() -> appDefaultLocale
                localeCode.contains('-') -> Locale.forLanguageTag(localeCode)
                else -> Locale(localeCode)
            }
            
            // Update default locale first—this affects NumberFormat, DateFormat, etc.
            Locale.setDefault(locale)
            
            // Now update the app's configuration
            val config = Configuration(context.resources.configuration)
            
            @Suppress("DEPRECATION")
            config.locale = locale
            
            // Create a new context with updated config
            val updatedContext = context.createConfigurationContext(config)
            
            // Also update the base context configuration for good measure
            @Suppress("DEPRECATION")
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
            
            Log.d(TAG, "Applied locale: ${locale.language} (index $languageIndex)")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to apply locale: ${e.message}", e)
        }
    }
    
    /**
     * Saves the selected language index to preferences.
     */
    fun saveLanguageIndex(index: Int) {
        prefs.edit().putInt(KEY_LANGUAGE_INDEX, index).apply()
        Log.d(TAG, "Saved language index: $index")
    }
    
    /**
     * Gets the currently saved language index.
     */
    fun getSavedLanguageIndex(): Int {
        return prefs.getInt(KEY_LANGUAGE_INDEX, DEFAULT_LANGUAGE_INDEX)
    }
    
    /**
     * Gets the number of available languages from resources.
     */
    fun getLanguageCount(): Int {
        return try {
            context.resources.getStringArray(
                com.prog7314.IdleLoadsheddingMobile.R.array.settings_languages
            ).size
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get language count", e)
            1  // Default to at least one language
        }
    }
    
    /**
     * Validates and clamps a language index to valid range.
     */
    fun validateLanguageIndex(index: Int): Int {
        val count = getLanguageCount()
        return index.coerceIn(0, count - 1)
    }
}
