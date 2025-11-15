package com.prog7314.IdleLoadsheddingMobile.ui.settings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.prog7314.IdleLoadsheddingMobile.ui.auth.Biometrics.BiometricRepository
import com.prog7314.IdleLoadsheddingMobile.ui.auth.Biometrics.BiometricRepositoryException
import com.prog7314.IdleLoadsheddingMobile.ui.auth.Biometrics.BiometricStatus
import com.prog7314.IdleLoadsheddingMobile.ui.auth.Biometrics.BiometricCryptoManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONObject
import javax.crypto.Cipher
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * ViewModel for BiometricSettingsFragment
 * Manages biometric authentication settings state and operations
 */
class BiometricSettingsViewModel(
    private val biometricRepository: BiometricRepository,
    private val cryptoManager: BiometricCryptoManager = BiometricCryptoManager(),
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    companion object {
        private const val TAG = "BiometricSettingsVM"
    }

    private val _biometricStatus = MutableStateFlow<BiometricSettingsUiState>(BiometricSettingsUiState.Loading)
    val biometricStatus: StateFlow<BiometricSettingsUiState> = _biometricStatus.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    /**
     * Load current biometric authentication status
     */
    fun loadBiometricStatus() {
        viewModelScope.launch {
            try {
                _biometricStatus.value = BiometricSettingsUiState.Loading
                
                val status = biometricRepository.getBiometricStatus()
                _biometricStatus.value = BiometricSettingsUiState.Success(
                    isEnabled = status.isEnabled,
                    userEmail = status.userEmail,
                    hasValidKey = status.hasValidKey,
                    hasStoredCredentials = status.hasStoredCredentials,
                    isFullyConfigured = status.isFullyConfigured()
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error loading biometric status", e)
                _biometricStatus.value = BiometricSettingsUiState.Error("Failed to load biometric status")
            }
        }
    }

    /**
     * Complete biometric enrollment after the user authenticates with the system prompt
     */
    fun completeBiometricEnrollment(email: String, cipher: Cipher) {
        viewModelScope.launch {
            try {
                val payload = createBiometricPayload(email)
                val encryptedData = cryptoManager.encryptData(payload, cipher)
                biometricRepository.storeEncryptedToken(encryptedData)
                biometricRepository.enableBiometricAuth(email)
                _message.value = "Biometric authentication enabled"
                loadBiometricStatus()
            } catch (e: BiometricRepositoryException) {
                Log.e(TAG, "Biometric repository error", e)
                _message.value = e.message ?: "Failed to complete biometric enrollment"
                _biometricStatus.value = BiometricSettingsUiState.Error(e.message ?: "Unknown error")
            } catch (e: Exception) {
                Log.e(TAG, "Error completing biometric enrollment", e)
                _message.value = e.message ?: "Failed to complete biometric enrollment"
                _biometricStatus.value = BiometricSettingsUiState.Error("Failed to finish biometric setup")
            }
        }
    }

    /**
     * Disable biometric authentication
     */
    fun disableBiometric() {
        viewModelScope.launch {
            try {
                biometricRepository.disableBiometricAuth()
                _message.value = "Biometric authentication disabled"
                loadBiometricStatus()
            } catch (e: BiometricRepositoryException) {
                Log.e(TAG, "Error disabling biometric", e)
                _message.value = "Failed to disable biometric authentication"
                _biometricStatus.value = BiometricSettingsUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    /**
     * Re-enroll biometric authentication
     * This clears existing data and allows user to set up again
     */
    fun reenrollBiometric() {
        viewModelScope.launch {
            try {
                val currentEmail = biometricRepository.getBiometricEmail()
                if (currentEmail != null) {
                    biometricRepository.disableBiometricAuth()
                    _message.value = "Biometric data cleared. Please enable again to re-enroll."
                    loadBiometricStatus()
                } else {
                    _message.value = "No user configured for biometric authentication"
                }
            } catch (e: BiometricRepositoryException) {
                Log.e(TAG, "Error re-enrolling biometric", e)
                _message.value = "Failed to re-enroll biometric authentication"
            }
        }
    }

    /**
     * Get current authenticated user email
     * This should be replaced with actual user management logic
     */
    fun getCurrentUserEmail(): String? = firebaseAuth.currentUser?.email

    private suspend fun fetchFreshIdToken(): String? = suspendCancellableCoroutine { continuation ->
        val user = firebaseAuth.currentUser
        if (user == null) {
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }

        user.getIdToken(true)
            .addOnSuccessListener { result ->
                continuation.resume(result.token)
            }
            .addOnFailureListener { exception ->
                if (continuation.isActive) {
                    continuation.resumeWithException(exception)
                }
            }
    }

    /**
     * Create biometric payload with user credentials
     * 
     * IMPORTANT: This method should be called DURING the initial login when the password
     * is available in memory. The password is encrypted with hardware-backed biometric keys.
     * 
     * @param email User's email
     * @param password User's password (only available during initial login)
     */
    fun createBiometricPayloadWithPassword(email: String, password: String): String {
        val user = firebaseAuth.currentUser
            ?: throw BiometricRepositoryException("No authenticated user available for biometric setup")

        val payload = JSONObject().apply {
            put("email", email)
            put("password", password) // Encrypted by BiometricCryptoManager
            put("uid", user.uid)
            put("generatedAt", System.currentTimeMillis())
        }
        return payload.toString()
    }

    private suspend fun createBiometricPayload(email: String): String {
        val user = firebaseAuth.currentUser
            ?: throw BiometricRepositoryException("No authenticated user available for biometric setup")

        // Fallback: metadata only (won't support auto re-authentication)
        val payload = JSONObject().apply {
            put("email", email)
            put("uid", user.uid)
            put("generatedAt", System.currentTimeMillis())
        }
        return payload.toString()
    }

    /**
     * Clear any displayed message
     */
    fun clearMessage() {
        _message.value = null
    }
}

/**
 * UI state for biometric settings
 */
sealed class BiometricSettingsUiState {
    object Loading : BiometricSettingsUiState()
    
    data class Success(
        val isEnabled: Boolean,
        val userEmail: String?,
        val hasValidKey: Boolean,
        val hasStoredCredentials: Boolean,
        val isFullyConfigured: Boolean
    ) : BiometricSettingsUiState()
    
    data class Error(val message: String) : BiometricSettingsUiState()
}

/**
 * ViewModelFactory for BiometricSettingsViewModel
 */
class BiometricSettingsViewModelFactory(
    private val biometricRepository: BiometricRepository,
    private val cryptoManager: BiometricCryptoManager = BiometricCryptoManager(),
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BiometricSettingsViewModel::class.java)) {
            return BiometricSettingsViewModel(biometricRepository, cryptoManager, firebaseAuth) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
