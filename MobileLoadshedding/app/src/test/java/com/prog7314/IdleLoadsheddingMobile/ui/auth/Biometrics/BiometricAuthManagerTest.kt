package com.prog7314.IdleLoadsheddingMobile.ui.auth.Biometrics

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for BiometricAuthManager
 * Tests biometric availability checking, prompt creation, and authentication flow
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class BiometricAuthManagerTest {

    private lateinit var authManager: BiometricAuthManager
    private lateinit var context: Context
    private lateinit var activity: FragmentActivity

    @Mock
    private lateinit var mockBiometricManager: BiometricManager

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        context = RuntimeEnvironment.getApplication()
        activity = Robolectric.buildActivity(FragmentActivity::class.java).create().get()
        authManager = BiometricAuthManager(context)
    }

    @Test
    fun `isBiometricAvailable returns correct availability states`() {
        // When
        val availability = authManager.isBiometricAvailable()

        // Then
        assertNotNull(availability)
        // In Robolectric, biometric hardware is typically not available
        // The exact state depends on Robolectric configuration
        assertTrue(
            availability is BiometricAvailability.NoHardware ||
            availability is BiometricAvailability.Available ||
            availability is BiometricAvailability.NoneEnrolled
        )
    }

    @Test
    fun `BiometricAvailability Available has no error message`() {
        // Given
        val availability = BiometricAvailability.Available

        // When/Then
        assertTrue(availability.isAvailable())
        assertNull(availability.getErrorMessage())
    }

    @Test
    fun `BiometricAvailability NoHardware has correct error message`() {
        // Given
        val availability = BiometricAvailability.NoHardware

        // When/Then
        assertFalse(availability.isAvailable())
        assertEquals("This device doesn't support biometric authentication", availability.getErrorMessage())
    }

    @Test
    fun `BiometricAvailability HardwareUnavailable has correct error message`() {
        // Given
        val availability = BiometricAvailability.HardwareUnavailable

        // When/Then
        assertFalse(availability.isAvailable())
        assertEquals("Biometric hardware is currently unavailable", availability.getErrorMessage())
    }

    @Test
    fun `BiometricAvailability NoneEnrolled has correct error message`() {
        // Given
        val availability = BiometricAvailability.NoneEnrolled

        // When/Then
        assertFalse(availability.isAvailable())
        assertEquals(
            "No biometric credentials enrolled. Please set up fingerprint or face recognition in Settings",
            availability.getErrorMessage()
        )
    }

    @Test
    fun `BiometricAvailability SecurityUpdateRequired has correct error message`() {
        // Given
        val availability = BiometricAvailability.SecurityUpdateRequired

        // When/Then
        assertFalse(availability.isAvailable())
        assertEquals("Security update required for biometric authentication", availability.getErrorMessage())
    }

    @Test
    fun `BiometricAvailability Unsupported has correct error message`() {
        // Given
        val availability = BiometricAvailability.Unsupported

        // When/Then
        assertFalse(availability.isAvailable())
        assertEquals("Biometric authentication is not supported", availability.getErrorMessage())
    }

    @Test
    fun `BiometricAvailability Unknown has correct error message`() {
        // Given
        val availability = BiometricAvailability.Unknown

        // When/Then
        assertFalse(availability.isAvailable())
        assertEquals("Unknown biometric status", availability.getErrorMessage())
    }

    @Test
    fun `createPromptInfo creates valid prompt with all parameters`() {
        // Given
        val title = "Test Title"
        val subtitle = "Test Subtitle"
        val description = "Test Description"
        val negativeButton = "Cancel Test"

        // When
        val promptInfo = authManager.createPromptInfo(
            title = title,
            subtitle = subtitle,
            description = description,
            negativeButtonText = negativeButton
        )

        // Then
        assertNotNull(promptInfo)
        // BiometricPrompt.PromptInfo doesn't expose getters, but we can verify it was created
    }

    @Test
    fun `createPromptInfo creates valid prompt with minimal parameters`() {
        // Given
        val title = "Minimal Title"

        // When
        val promptInfo = authManager.createPromptInfo(title = title)

        // Then
        assertNotNull(promptInfo)
    }

    @Test
    fun `createBiometricPrompt creates valid prompt with callbacks`() {
        // Given
        var successCalled = false
        var errorCalled = false
        var failedCalled = false

        val onSuccess: (BiometricPrompt.AuthenticationResult) -> Unit = { successCalled = true }
        val onError: (Int, String) -> Unit = { _, _ -> errorCalled = true }
        val onFailed: () -> Unit = { failedCalled = true }

        // When
        val biometricPrompt = authManager.createBiometricPrompt(
            activity = activity,
            onSuccess = onSuccess,
            onError = onError,
            onFailed = onFailed
        )

        // Then
        assertNotNull(biometricPrompt)
        // Callbacks can't be directly tested without triggering actual biometric authentication
    }

    @Test
    fun `createEncryptionCryptoObject creates valid crypto object`() {
        // Given
        val cryptoManager = BiometricCryptoManager()

        // When
        val cryptoObject = cryptoManager.createEncryptionCryptoObject()

        // Then
        assertNotNull(cryptoObject)
        assertNotNull(cryptoObject.cipher)
    }

    @Test
    fun `isStrongBiometricAvailable checks strong biometric support`() {
        // When
        val available = authManager.isStrongBiometricAvailable()

        // Then
        // Result depends on Robolectric environment
        assertNotNull(available)
    }

    @Test
    fun `BiometricAvailability sealed class covers all states`() {
        // Test that all availability states can be created
        val states = listOf(
            BiometricAvailability.Available,
            BiometricAvailability.NoHardware,
            BiometricAvailability.HardwareUnavailable,
            BiometricAvailability.NoneEnrolled,
            BiometricAvailability.SecurityUpdateRequired,
            BiometricAvailability.Unsupported,
            BiometricAvailability.Unknown
        )

        states.forEach { state ->
            assertNotNull(state)
            // Verify error message behavior
            if (state is BiometricAvailability.Available) {
                assertNull(state.getErrorMessage())
            } else {
                assertNotNull(state.getErrorMessage())
            }
        }
    }
}

private fun assertNull(value: Any?) {
    if (value != null) {
        throw AssertionError("Expected null but was: $value")
    }
}
