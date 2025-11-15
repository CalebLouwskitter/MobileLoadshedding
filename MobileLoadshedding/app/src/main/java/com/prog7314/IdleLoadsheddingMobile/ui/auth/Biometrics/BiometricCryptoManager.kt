package com.prog7314.IdleLoadsheddingMobile.ui.auth.Biometrics

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import androidx.biometric.BiometricPrompt
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * BiometricCryptoManager handles secure encryption/decryption of data using Android Keystore
 * and biometric authentication. This ensures that sensitive data (like authentication tokens)
 * are protected by biometric credentials and never stored in plaintext.
 */
class BiometricCryptoManager {
    
    companion object {
        private const val TAG = "BiometricCryptoManager"
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val KEY_NAME = "MobileLoadsheddingBiometricKey"
        private const val TRANSFORMATION = "${KeyProperties.KEY_ALGORITHM_AES}/${KeyProperties.BLOCK_MODE_GCM}/${KeyProperties.ENCRYPTION_PADDING_NONE}"
        private const val GCM_TAG_LENGTH = 128
    }

    private val keyStore: KeyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply {
        load(null)
    }

    /**
     * Generate or retrieve the secret key from Android Keystore
     * This key requires biometric authentication to use
     */
    private fun getOrCreateSecretKey(): SecretKey {
        // Try to get existing key first
        keyStore.getKey(KEY_NAME, null)?.let { return it as SecretKey }

        // Generate new key if it doesn't exist
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            KEYSTORE_PROVIDER
        )

        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            KEY_NAME,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setUserAuthenticationRequired(true)
            .setInvalidatedByBiometricEnrollment(true) // Key becomes invalid if biometrics change
            .build()

        keyGenerator.init(keyGenParameterSpec)
        return keyGenerator.generateKey()
    }

    /**
     * Get a cipher for encryption
     * This cipher must be authenticated via BiometricPrompt before use
     */
    fun getEncryptionCipher(): Cipher {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val secretKey = getOrCreateSecretKey()
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        return cipher
    }

    /**
     * Get a cipher for decryption
     * This cipher must be authenticated via BiometricPrompt before use
     * 
     * @param iv The initialization vector used during encryption
     */
    fun getDecryptionCipher(iv: ByteArray): Cipher {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val secretKey = getOrCreateSecretKey()
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
        return cipher
    }

    /**
     * Encrypt data using an authenticated cipher
     * Should be called after successful biometric authentication
     * 
     * @param plaintext The data to encrypt
     * @param cipher The cipher authenticated via BiometricPrompt
     * @return EncryptedData containing the ciphertext and IV
     */
    fun encryptData(plaintext: String, cipher: Cipher): EncryptedData {
        try {
            val plaintextBytes = plaintext.toByteArray(Charsets.UTF_8)
            val ciphertext = cipher.doFinal(plaintextBytes)
            val iv = cipher.iv

            return EncryptedData(
                ciphertext = Base64.encodeToString(ciphertext, Base64.NO_WRAP),
                iv = Base64.encodeToString(iv, Base64.NO_WRAP)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Encryption failed", e)
            throw BiometricCryptoException("Failed to encrypt data", e)
        }
    }

    /**
     * Decrypt data using an authenticated cipher
     * Should be called after successful biometric authentication
     * 
     * @param encryptedData The encrypted data containing ciphertext and IV
     * @param cipher The cipher authenticated via BiometricPrompt
     * @return The decrypted plaintext
     */
    fun decryptData(encryptedData: EncryptedData, cipher: Cipher): String {
        try {
            val ciphertext = Base64.decode(encryptedData.ciphertext, Base64.NO_WRAP)
            val plaintextBytes = cipher.doFinal(ciphertext)
            return String(plaintextBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            Log.e(TAG, "Decryption failed", e)
            throw BiometricCryptoException("Failed to decrypt data", e)
        }
    }

    /**
     * Delete the encryption key from the keystore
     * Call this when user disables biometric authentication
     */
    fun deleteKey() {
        try {
            if (keyStore.containsAlias(KEY_NAME)) {
                keyStore.deleteEntry(KEY_NAME)
                Log.d(TAG, "Biometric key deleted")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete key", e)
        }
    }

    /**
     * Check if the biometric key exists and is valid
     */
    fun isKeyAvailable(): Boolean {
        return try {
            keyStore.containsAlias(KEY_NAME)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking key availability", e)
            false
        }
    }

    /**
     * Create a BiometricPrompt.CryptoObject for encryption
     */
    fun createEncryptionCryptoObject(): BiometricPrompt.CryptoObject {
        return BiometricPrompt.CryptoObject(getEncryptionCipher())
    }

    /**
     * Create a BiometricPrompt.CryptoObject for decryption
     */
    fun createDecryptionCryptoObject(iv: ByteArray): BiometricPrompt.CryptoObject {
        return BiometricPrompt.CryptoObject(getDecryptionCipher(iv))
    }
}

/**
 * Data class representing encrypted data with its initialization vector
 */
data class EncryptedData(
    val ciphertext: String,
    val iv: String
)

/**
 * Exception thrown when cryptographic operations fail
 */
class BiometricCryptoException(message: String, cause: Throwable? = null) : Exception(message, cause)
