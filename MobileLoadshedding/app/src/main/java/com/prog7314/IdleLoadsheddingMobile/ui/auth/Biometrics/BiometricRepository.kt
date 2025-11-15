package com.prog7314.IdleLoadsheddingMobile.ui.auth.Biometrics

import android.content.Context
import android.util.Base64
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.prog7314.IdleLoadsheddingMobile.auth.AuthPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

// Singleton DataStore instance to avoid multiple instances for the same file
private val Context.biometricDataStore: DataStore<Preferences> by preferencesDataStore(name = "biometric_secure_data")

/**
 * BiometricRepository manages biometric authentication state and encrypted credential storage.
 * It acts as a single source of truth for biometric-related data and operations.
 */
class BiometricRepository(
    private val context: Context,
    private val cryptoManager: BiometricCryptoManager = BiometricCryptoManager(),
    private val authPreferences: AuthPreferences = AuthPreferences(context)
) {
    
    companion object {
        private const val TAG = "BiometricRepository"
    }

    private object Keys {
        val encryptedToken = stringPreferencesKey("encrypted_token")
        val tokenIv = stringPreferencesKey("token_iv")
    }

    /**
     * Check if biometric authentication is configured and ready to use
     */
    suspend fun isBiometricConfigured(): Boolean {
        val enabled = authPreferences.biometricEnabled.first()
        val hasKey = cryptoManager.isKeyAvailable()
        return enabled && hasKey
    }

    /**
     * Get the email associated with biometric authentication
     */
    suspend fun getBiometricEmail(): String? {
        val email = authPreferences.biometricEmail.first()
        return email.takeIf { it.isNotBlank() }
    }

    /**
     * Enable biometric authentication for a user
     * This stores the email but does NOT store any tokens yet
     * 
     * @param email The user's email address
     */
    suspend fun enableBiometricAuth(email: String) {
        try {
            authPreferences.setBiometricEnabled(true, email)
            Log.d(TAG, "Biometric authentication enabled for user")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to enable biometric authentication", e)
            throw BiometricRepositoryException("Failed to enable biometric authentication", e)
        }
    }

    /**
     * Store an encrypted authentication token
     * This should be called after successful biometric authentication with a crypto object
     * 
     * @param token The authentication token to encrypt and store
     * @param encryptedData The encrypted data (already encrypted using BiometricCryptoManager)
     */
    suspend fun storeEncryptedToken(encryptedData: EncryptedData) {
        try {
            context.biometricDataStore.edit { preferences ->
                preferences[Keys.encryptedToken] = encryptedData.ciphertext
                preferences[Keys.tokenIv] = encryptedData.iv
            }
            Log.d(TAG, "Encrypted token stored successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to store encrypted token", e)
            throw BiometricRepositoryException("Failed to store encrypted token", e)
        }
    }

    /**
     * Retrieve encrypted token data
     * Returns null if no token is stored
     */
    suspend fun getEncryptedToken(): EncryptedData? {
        return try {
            val preferences = context.biometricDataStore.data.first()
            val ciphertext = preferences[Keys.encryptedToken]
            val iv = preferences[Keys.tokenIv]

            if (ciphertext != null && iv != null) {
                EncryptedData(ciphertext, iv)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to retrieve encrypted token", e)
            null
        }
    }

    /**
     * Check if an encrypted token is stored
     */
    suspend fun hasStoredToken(): Boolean {
        return getEncryptedToken() != null
    }

    /**
     * Disable biometric authentication and clear all biometric data
     * This removes the encryption key, stored tokens, and preferences
     */
    suspend fun disableBiometricAuth() {
        try {
            // Clear encrypted data
            clearEncryptedData()
            
            // Delete the encryption key
            cryptoManager.deleteKey()
            
            // Clear preferences
            authPreferences.clearBiometricData()
            
            Log.d(TAG, "Biometric authentication disabled and data cleared")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to disable biometric authentication", e)
            throw BiometricRepositoryException("Failed to disable biometric authentication", e)
        }
    }

    /**
     * Clear stored encrypted data without disabling biometric authentication
     * Useful for logout scenarios
     */
    suspend fun clearEncryptedData() {
        try {
            context.biometricDataStore.edit { preferences ->
                preferences.remove(Keys.encryptedToken)
                preferences.remove(Keys.tokenIv)
            }
            Log.d(TAG, "Encrypted data cleared")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear encrypted data", e)
            throw BiometricRepositoryException("Failed to clear encrypted data", e)
        }
    }

    /**
     * Handle biometric enrollment change
     * Called when the system notifies that biometric credentials have changed
     * This invalidates the existing key and clears data
     */
    suspend fun handleBiometricEnrollmentChange() {
        try {
            Log.w(TAG, "Biometric enrollment changed - clearing biometric data")
            disableBiometricAuth()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to handle biometric enrollment change", e)
        }
    }

    /**
     * Get biometric authentication status information
     */
    suspend fun getBiometricStatus(): BiometricStatus {
        val enabled = authPreferences.biometricEnabled.first()
        val email = getBiometricEmail()
        val hasKey = cryptoManager.isKeyAvailable()
        val hasToken = hasStoredToken()

        return BiometricStatus(
            isEnabled = enabled,
            userEmail = email,
            hasValidKey = hasKey,
            hasStoredCredentials = hasToken
        )
    }
}

/**
 * Data class representing the current biometric authentication status
 */
data class BiometricStatus(
    val isEnabled: Boolean,
    val userEmail: String?,
    val hasValidKey: Boolean,
    val hasStoredCredentials: Boolean
) {
    /**
     * Check if biometric authentication is fully configured and ready to use
     */
    fun isFullyConfigured(): Boolean {
        return isEnabled && userEmail != null && hasValidKey
    }
}

/**
 * Exception thrown by BiometricRepository operations
 */
class BiometricRepositoryException(message: String, cause: Throwable? = null) : Exception(message, cause)
