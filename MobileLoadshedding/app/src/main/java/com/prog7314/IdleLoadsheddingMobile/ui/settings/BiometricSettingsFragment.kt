package com.prog7314.IdleLoadsheddingMobile.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.prog7314.IdleLoadsheddingMobile.R
import com.prog7314.IdleLoadsheddingMobile.databinding.FragmentBiometricSettingsBinding
import com.prog7314.IdleLoadsheddingMobile.ui.auth.Biometrics.BiometricAuthManager
import com.prog7314.IdleLoadsheddingMobile.ui.auth.Biometrics.BiometricCryptoManager
import com.prog7314.IdleLoadsheddingMobile.ui.auth.Biometrics.BiometricPromptHelper
import com.prog7314.IdleLoadsheddingMobile.ui.auth.Biometrics.BiometricRepository
import kotlinx.coroutines.launch

/**
 * Fragment for managing biometric authentication settings
 * Allows users to enable/disable biometric login and view status
 */
class BiometricSettingsFragment : Fragment() {

    private var _binding: FragmentBiometricSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: BiometricSettingsViewModel
    private lateinit var biometricAuthManager: BiometricAuthManager
    private lateinit var biometricPromptHelper: BiometricPromptHelper
    private val biometricCryptoManager by lazy { BiometricCryptoManager() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBiometricSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize managers
        biometricAuthManager = BiometricAuthManager(requireContext())
        biometricPromptHelper = BiometricPromptHelper(requireActivity(), biometricAuthManager)

        // Initialize ViewModel
        val repository = BiometricRepository(requireContext())
        val factory = BiometricSettingsViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[BiometricSettingsViewModel::class.java]

        setupUI()
        observeViewModel()
        loadBiometricStatus()
    }

    private fun setupUI() {
        binding.switchBiometric.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                enableBiometricAuthentication()
            } else {
                disableBiometricAuthentication()
            }
        }

        binding.buttonTestBiometric.setOnClickListener {
            testBiometricAuthentication()
        }

        binding.buttonReenroll.setOnClickListener {
            reenrollBiometric()
        }

        // In-UI back arrow to close the biometric settings screen
        binding.root.findViewById<View?>(R.id.btn_back_biometric)?.setOnClickListener {
            requireActivity().onBackPressed()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.biometricStatus.collect { status ->
                updateUI(status)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.message.collect { message ->
                message?.let {
                    Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                    viewModel.clearMessage()
                }
            }
        }
    }

    private fun loadBiometricStatus() {
        viewModel.loadBiometricStatus()
    }

    private fun updateUI(status: BiometricSettingsUiState) {
        when (status) {
            is BiometricSettingsUiState.Loading -> {
                binding.progressBar.visibility = View.VISIBLE
                binding.contentLayout.visibility = View.GONE
            }
            is BiometricSettingsUiState.Success -> {
                binding.progressBar.visibility = View.GONE
                binding.contentLayout.visibility = View.VISIBLE

                // Update switch without triggering listener
                binding.switchBiometric.setOnCheckedChangeListener(null)
                binding.switchBiometric.isChecked = status.isEnabled
                binding.switchBiometric.setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        enableBiometricAuthentication()
                    } else {
                        disableBiometricAuthentication()
                    }
                }

                // Update status text
                binding.textStatus.text = when {
                    status.isFullyConfigured -> "✓ Biometric authentication is active and ready"
                    status.isEnabled -> "⚠ Biometric enabled but not fully configured"
                    else -> "Biometric authentication is disabled"
                }

                // Update user email
                binding.textEmail.text = status.userEmail ?: "No user configured"

                // Update hardware status
                val availability = biometricAuthManager.isBiometricAvailable()
                binding.textHardwareStatus.text = if (availability.isAvailable()) {
                    "✓ Biometric hardware available"
                } else {
                    "✗ ${availability.getErrorMessage()}"
                }

                // Enable/disable buttons based on status
                binding.buttonTestBiometric.isEnabled = status.isEnabled && availability.isAvailable()
                binding.buttonReenroll.isEnabled = status.isEnabled
                binding.switchBiometric.isEnabled = availability.isAvailable()
            }
            is BiometricSettingsUiState.Error -> {
                binding.progressBar.visibility = View.GONE
                binding.contentLayout.visibility = View.VISIBLE
                Toast.makeText(requireContext(), status.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun enableBiometricAuthentication() {
        val availability = biometricAuthManager.isBiometricAvailable()
        
        if (!availability.isAvailable()) {
            Toast.makeText(
                requireContext(),
                availability.getErrorMessage() ?: "Biometric not available",
                Toast.LENGTH_LONG
            ).show()
            binding.switchBiometric.isChecked = false
            return
        }

        // Get current user email (you may need to retrieve this from your auth system)
        val userEmail = viewModel.getCurrentUserEmail()
        
        if (userEmail.isNullOrBlank()) {
            Toast.makeText(
                requireContext(),
                "Please log in first to enable biometric authentication",
                Toast.LENGTH_LONG
            ).show()
            binding.switchBiometric.isChecked = false
            return
        }

        val cryptoObject = try {
            biometricCryptoManager.createEncryptionCryptoObject()
        } catch (e: Exception) {
            Toast.makeText(
                requireContext(),
                "Unable to start biometric enrollment: ${e.localizedMessage}",
                Toast.LENGTH_LONG
            ).show()
            binding.switchBiometric.isChecked = false
            return
        }
        biometricPromptHelper.authenticateWithCrypto(
            cryptoObject = cryptoObject,
            title = "Enable Biometric Authentication",
            subtitle = "Confirm your identity",
            description = "Use your biometric credential to secure this device.",
            onSuccess = { result ->
                val cipher = result.cryptoObject?.cipher
                if (cipher == null) {
                    Toast.makeText(
                        requireContext(),
                        "Unable to capture cryptographic data. Please try again.",
                        Toast.LENGTH_LONG
                    ).show()
                    binding.switchBiometric.isChecked = false
                } else {
                    viewLifecycleOwner.lifecycleScope.launch {
                        viewModel.completeBiometricEnrollment(userEmail, cipher)
                    }
                }
            },
            onError = { errorMessage ->
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
                binding.switchBiometric.isChecked = false
            },
            onCancelled = {
                Toast.makeText(requireContext(), "Authentication cancelled", Toast.LENGTH_SHORT).show()
                binding.switchBiometric.isChecked = false
            }
        )
    }

    private fun disableBiometricAuthentication() {
        viewModel.disableBiometric()
    }

    private fun testBiometricAuthentication() {
        biometricPromptHelper.authenticate(
            title = "Test Biometric Authentication",
            subtitle = "Verify your biometric credential",
            description = "This is a test to ensure biometric authentication is working correctly",
            onSuccess = {
                Toast.makeText(
                    requireContext(),
                    "✓ Biometric authentication successful!",
                    Toast.LENGTH_SHORT
                ).show()
            },
            onError = { errorMessage ->
                Toast.makeText(
                    requireContext(),
                    "Error: $errorMessage",
                    Toast.LENGTH_LONG
                ).show()
            },
            onCancelled = {
                Toast.makeText(
                    requireContext(),
                    "Authentication cancelled",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
    }

    private fun reenrollBiometric() {
        // Re-enrollment requires disabling and re-enabling
        viewModel.reenrollBiometric()
        Toast.makeText(
            requireContext(),
            "Please toggle biometric authentication to re-enroll",
            Toast.LENGTH_LONG
        ).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
