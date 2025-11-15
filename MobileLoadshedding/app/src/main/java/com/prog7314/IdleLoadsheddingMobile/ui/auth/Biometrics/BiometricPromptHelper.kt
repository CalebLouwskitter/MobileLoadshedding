package com.prog7314.IdleLoadsheddingMobile.ui.auth.Biometrics

import android.util.Log
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity

/**
 * BiometricPromptHelper simplifies biometric authentication flow
 * and provides easy-to-use callbacks for authentication results.
 */
class BiometricPromptHelper(
    private val activity: FragmentActivity,
    private val biometricAuthManager: BiometricAuthManager
) {
    private companion object {
        const val TAG = "BiometricPromptHelper"
    }

    /**
     * Authenticate user with biometrics
     * 
     * @param title Title shown in the biometric prompt
     * @param subtitle Optional subtitle
     * @param description Optional description
     * @param onSuccess Callback when authentication succeeds
     * @param onError Callback when authentication error occurs
     * @param onCancelled Callback when user cancels authentication
     */
    fun authenticate(
        title: String = "Biometric Authentication",
        subtitle: String? = "Verify your identity",
        description: String? = null,
        onSuccess: () -> Unit,
        onError: (errorMessage: String) -> Unit,
        onCancelled: () -> Unit = {}
    ) {
        try {
            // Check if activity is still valid
            if (activity.isFinishing || activity.isDestroyed) {
                Log.w(TAG, "Activity is finishing or destroyed, cannot show biometric prompt")
                onError("Cannot authenticate - activity not available")
                return
            }

            val availability = biometricAuthManager.isBiometricAvailable()
            
            if (!availability.isAvailable()) {
                val errorMessage = availability.getErrorMessage() ?: "Biometric authentication not available"
                Log.w(TAG, "Biometric not available: $errorMessage")
                onError(errorMessage)
                return
            }

            val biometricPrompt = biometricAuthManager.createBiometricPrompt(
                activity = activity,
                onSuccess = { result ->
                    Log.d(TAG, "Authentication succeeded")
                    try {
                        onSuccess()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in success callback", e)
                    }
            },
            onError = { errorCode, errorMessage ->
                Log.e(TAG, "Authentication error: $errorCode - $errorMessage")
                when (errorCode) {
                    BiometricPrompt.ERROR_USER_CANCELED,
                    BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                    BiometricPrompt.ERROR_CANCELED -> {
                        Log.d(TAG, "Authentication cancelled by user")
                        onCancelled()
                    }
                    BiometricPrompt.ERROR_LOCKOUT,
                    BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> {
                        onError("Too many attempts. Please try again later or use password.")
                    }
                    BiometricPrompt.ERROR_NO_BIOMETRICS -> {
                        onError("No biometric credentials enrolled. Please set up biometrics in Settings.")
                    }
                    BiometricPrompt.ERROR_HW_NOT_PRESENT -> {
                        onError("This device doesn't support biometric authentication.")
                    }
                    BiometricPrompt.ERROR_HW_UNAVAILABLE -> {
                        onError("Biometric hardware is currently unavailable.")
                    }
                    else -> {
                        onError(errorMessage)
                    }
                }
            },
            onFailed = {
                Log.w(TAG, "Authentication failed (not recognized)")
                // Don't call onError here - let user try again
            }
        )

            val promptInfo = biometricAuthManager.createPromptInfo(
                title = title,
                subtitle = subtitle,
                description = description,
                negativeButtonText = "Cancel"
            )

            biometricPrompt.authenticate(promptInfo)
        } catch (e: Exception) {
            Log.e(TAG, "Exception during biometric authentication", e)
            onError("Authentication failed: ${e.localizedMessage ?: "Unknown error"}")
        }
    }

    /**
     * Check if biometric authentication is available and return status message
     */
    fun checkAvailability(): Pair<Boolean, String?> {
        val availability = biometricAuthManager.isBiometricAvailable()
        return Pair(availability.isAvailable(), availability.getErrorMessage())
    }

    /**
     * Authenticate with a CryptoObject for encryption/decryption operations
     * 
     * @param cryptoObject The CryptoObject containing the cipher for encryption/decryption
     * @param title Title shown in the biometric prompt
     * @param subtitle Optional subtitle
     * @param description Optional description
     * @param onSuccess Callback with the authenticated result containing the crypto object
     * @param onError Callback when authentication error occurs
     * @param onCancelled Callback when user cancels authentication
     */
    fun authenticateWithCrypto(
        cryptoObject: BiometricPrompt.CryptoObject,
        title: String = "Biometric Authentication",
        subtitle: String? = "Verify your identity",
        description: String? = null,
        onSuccess: (BiometricPrompt.AuthenticationResult) -> Unit,
        onError: (errorMessage: String) -> Unit,
        onCancelled: () -> Unit = {}
    ) {
        try {
            // Check if activity is still valid
            if (activity.isFinishing || activity.isDestroyed) {
                Log.w(TAG, "Activity is finishing or destroyed, cannot show biometric prompt")
                onError("Cannot authenticate - activity not available")
                return
            }

            val availability = biometricAuthManager.isBiometricAvailable()
            
            if (!availability.isAvailable()) {
                val errorMessage = availability.getErrorMessage() ?: "Biometric authentication not available"
                Log.w(TAG, "Biometric not available: $errorMessage")
                onError(errorMessage)
                return
            }

            val promptInfo = biometricAuthManager.createPromptInfo(
                title = title,
                subtitle = subtitle,
                description = description,
                negativeButtonText = "Cancel"
            )

            biometricAuthManager.authenticateWithCrypto(
                activity = activity,
                cryptoObject = cryptoObject,
                promptInfo = promptInfo,
                onSuccess = { result ->
                    Log.d(TAG, "Crypto authentication succeeded")
                    try {
                        onSuccess(result)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in success callback", e)
                        onError("Authentication succeeded but callback failed: ${e.message}")
                    }
                },
                onError = { errorCode, errorMessage ->
                    Log.e(TAG, "Crypto authentication error: $errorCode - $errorMessage")
                    when (errorCode) {
                        BiometricPrompt.ERROR_USER_CANCELED,
                        BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                        BiometricPrompt.ERROR_CANCELED -> {
                            Log.d(TAG, "Crypto authentication cancelled by user")
                            onCancelled()
                        }
                        BiometricPrompt.ERROR_LOCKOUT,
                        BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> {
                            onError("Too many attempts. Please try again later or use password.")
                        }
                        BiometricPrompt.ERROR_NO_BIOMETRICS -> {
                            onError("No biometric credentials enrolled. Please set up biometrics in Settings.")
                        }
                        BiometricPrompt.ERROR_HW_NOT_PRESENT -> {
                            onError("This device doesn't support biometric authentication.")
                        }
                        BiometricPrompt.ERROR_HW_UNAVAILABLE -> {
                            onError("Biometric hardware is currently unavailable.")
                        }
                        else -> {
                            onError(errorMessage)
                        }
                    }
                },
                onFailed = {
                    Log.w(TAG, "Crypto authentication failed (not recognized)")
                    // Don't call onError here - let user try again
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Exception during crypto authentication", e)
            onError("Authentication error: ${e.message}")
        }
    }
}
