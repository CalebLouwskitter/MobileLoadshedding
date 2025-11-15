package com.prog7314.IdleLoadsheddingMobile.ui.auth.Biometrics

import android.content.Context
import com.prog7314.IdleLoadsheddingMobile.auth.AuthPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.never
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for BiometricRepository
 * Tests data management, encryption handling, and state management
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class BiometricRepositoryTest {

    private lateinit var repository: BiometricRepository
    private lateinit var context: Context
    
    @Mock
    private lateinit var mockCryptoManager: BiometricCryptoManager
    
    @Mock
    private lateinit var mockAuthPreferences: AuthPreferences

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        context = RuntimeEnvironment.getApplication()
        
        // Use real implementations for integration-like testing
        repository = BiometricRepository(
            context = context,
            cryptoManager = BiometricCryptoManager(),
            authPreferences = AuthPreferences(context)
        )
    }

    @After
    fun tearDown() = runTest {
        // Clean up after tests
        try {
            repository.disableBiometricAuth()
        } catch (e: Exception) {
            // Ignore cleanup errors
        }
    }

    @Test
    fun `enableBiometricAuth stores user email`() = runTest {
        // Given
        val email = "test@example.com"

        // When
        repository.enableBiometricAuth(email)

        // Then
        val storedEmail = repository.getBiometricEmail()
        assertEquals(email, storedEmail)
    }

    @Test
    fun `isBiometricConfigured returns false when not configured`() = runTest {
        // When
        val configured = repository.isBiometricConfigured()

        // Then
        assertFalse(configured)
    }

    @Test
    fun `isBiometricConfigured returns true when fully configured`() = runTest {
        // Given
        val email = "user@example.com"
        repository.enableBiometricAuth(email)
        
        // Create a key by getting encryption cipher
        val cryptoManager = BiometricCryptoManager()
        cryptoManager.getEncryptionCipher()

        // When
        val configured = repository.isBiometricConfigured()

        // Then
        assertTrue(configured)
    }

    @Test
    fun `storeEncryptedToken successfully stores data`() = runTest {
        // Given
        val encryptedData = EncryptedData(
            ciphertext = "encrypted_test_data",
            iv = "test_iv_12345"
        )

        // When
        repository.storeEncryptedToken(encryptedData)

        // Then
        val retrieved = repository.getEncryptedToken()
        assertNotNull(retrieved)
        assertEquals(encryptedData.ciphertext, retrieved.ciphertext)
        assertEquals(encryptedData.iv, retrieved.iv)
    }

    @Test
    fun `getEncryptedToken returns null when no token stored`() = runTest {
        // When
        val token = repository.getEncryptedToken()

        // Then
        assertNull(token)
    }

    @Test
    fun `hasStoredToken returns false when no token stored`() = runTest {
        // When
        val hasToken = repository.hasStoredToken()

        // Then
        assertFalse(hasToken)
    }

    @Test
    fun `hasStoredToken returns true when token is stored`() = runTest {
        // Given
        val encryptedData = EncryptedData("ciphertext", "iv")
        repository.storeEncryptedToken(encryptedData)

        // When
        val hasToken = repository.hasStoredToken()

        // Then
        assertTrue(hasToken)
    }

    @Test
    fun `clearEncryptedData removes stored token`() = runTest {
        // Given
        val encryptedData = EncryptedData("test_cipher", "test_iv")
        repository.storeEncryptedToken(encryptedData)
        assertTrue(repository.hasStoredToken())

        // When
        repository.clearEncryptedData()

        // Then
        assertFalse(repository.hasStoredToken())
        assertNull(repository.getEncryptedToken())
    }

    @Test
    fun `disableBiometricAuth clears all biometric data`() = runTest {
        // Given
        val email = "test@example.com"
        repository.enableBiometricAuth(email)
        val encryptedData = EncryptedData("cipher", "iv")
        repository.storeEncryptedToken(encryptedData)

        // When
        repository.disableBiometricAuth()

        // Then
        assertFalse(repository.hasStoredToken())
        assertNull(repository.getBiometricEmail())
        assertFalse(repository.isBiometricConfigured())
    }

    @Test
    fun `getBiometricStatus returns correct status when not configured`() = runTest {
        // When
        val status = repository.getBiometricStatus()

        // Then
        assertFalse(status.isEnabled)
        assertNull(status.userEmail)
        assertFalse(status.hasValidKey)
        assertFalse(status.hasStoredCredentials)
        assertFalse(status.isFullyConfigured())
    }

    @Test
    fun `getBiometricStatus returns correct status when configured`() = runTest {
        // Given
        val email = "configured@example.com"
        repository.enableBiometricAuth(email)
        
        // Create key
        val cryptoManager = BiometricCryptoManager()
        cryptoManager.getEncryptionCipher()
        
        val encryptedData = EncryptedData("cipher", "iv")
        repository.storeEncryptedToken(encryptedData)

        // When
        val status = repository.getBiometricStatus()

        // Then
        assertTrue(status.isEnabled)
        assertEquals(email, status.userEmail)
        assertTrue(status.hasValidKey)
        assertTrue(status.hasStoredCredentials)
        assertTrue(status.isFullyConfigured())
    }

    @Test
    fun `handleBiometricEnrollmentChange disables biometric auth`() = runTest {
        // Given
        val email = "enrolled@example.com"
        repository.enableBiometricAuth(email)
        val encryptedData = EncryptedData("cipher", "iv")
        repository.storeEncryptedToken(encryptedData)

        // When
        repository.handleBiometricEnrollmentChange()

        // Then
        assertFalse(repository.isBiometricConfigured())
        assertFalse(repository.hasStoredToken())
    }

    @Test
    fun `multiple store operations update token correctly`() = runTest {
        // Given
        val firstData = EncryptedData("first_cipher", "first_iv")
        val secondData = EncryptedData("second_cipher", "second_iv")

        // When
        repository.storeEncryptedToken(firstData)
        val firstRetrieved = repository.getEncryptedToken()
        
        repository.storeEncryptedToken(secondData)
        val secondRetrieved = repository.getEncryptedToken()

        // Then
        assertEquals(firstData.ciphertext, firstRetrieved?.ciphertext)
        assertEquals(secondData.ciphertext, secondRetrieved?.ciphertext)
    }

    @Test
    fun `getBiometricEmail returns null for blank email`() = runTest {
        // Given
        repository.enableBiometricAuth("")

        // When
        val email = repository.getBiometricEmail()

        // Then
        assertNull(email)
    }

    @Test
    fun `BiometricStatus isFullyConfigured checks all requirements`() {
        // Test various combinations
        val notConfigured = BiometricStatus(
            isEnabled = false,
            userEmail = null,
            hasValidKey = false,
            hasStoredCredentials = false
        )
        assertFalse(notConfigured.isFullyConfigured())

        val missingEmail = BiometricStatus(
            isEnabled = true,
            userEmail = null,
            hasValidKey = true,
            hasStoredCredentials = true
        )
        assertFalse(missingEmail.isFullyConfigured())

        val missingKey = BiometricStatus(
            isEnabled = true,
            userEmail = "test@example.com",
            hasValidKey = false,
            hasStoredCredentials = true
        )
        assertFalse(missingKey.isFullyConfigured())

        val fullyConfigured = BiometricStatus(
            isEnabled = true,
            userEmail = "test@example.com",
            hasValidKey = true,
            hasStoredCredentials = true
        )
        assertTrue(fullyConfigured.isFullyConfigured())
    }

    @Test
    fun `concurrent operations handle state correctly`() = runTest {
        // Given
        val email = "concurrent@example.com"
        
        // When - Perform multiple operations
        repository.enableBiometricAuth(email)
        repository.storeEncryptedToken(EncryptedData("cipher1", "iv1"))
        val status1 = repository.getBiometricStatus()
        
        repository.clearEncryptedData()
        val status2 = repository.getBiometricStatus()
        
        repository.storeEncryptedToken(EncryptedData("cipher2", "iv2"))
        val status3 = repository.getBiometricStatus()

        // Then
        assertTrue(status1.hasStoredCredentials)
        assertFalse(status2.hasStoredCredentials)
        assertTrue(status3.hasStoredCredentials)
    }
}
