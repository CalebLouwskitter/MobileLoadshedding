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
                    Toast.makeText(requireContext(), getLocalizedMessage(it), Toast.LENGTH_SHORT).show()
                    viewModel.clearMessage()
                }
            }
        }
    }

    private fun getLocalizedMessage(message: String): String {
        return when (message) {
            "Biometric authentication enabled" -> getString(R.string.biometric_msg_enabled)
            "Biometric authentication disabled" -> getString(R.string.biometric_msg_disabled)
            "Biometric data cleared. Please enable again to re-enroll." -> getString(R.string.biometric_msg_cleared)
            "No user configured for biometric authentication" -> getString(R.string.biometric_error_no_user)
            "Failed to complete biometric enrollment" -> getString(R.string.biometric_error_enrollment_failed)
            "Failed to finish biometric setup" -> getString(R.string.biometric_error_setup_failed)
            "Failed to disable biometric authentication" -> getString(R.string.biometric_error_disable_failed)
            "Failed to re-enroll biometric authentication" -> getString(R.string.biometric_error_reenroll_failed)
            "Failed to load biometric status" -> getString(R.string.biometric_error_loading)
            else -> message
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
                    status.isFullyConfigured -> getString(R.string.biometric_status_active)
                    status.isEnabled -> getString(R.string.biometric_status_not_configured)
                    else -> getString(R.string.biometric_status_disabled)
                }

                // Update user email
                binding.textEmail.text = status.userEmail ?: getString(R.string.biometric_no_user)

                // Update hardware status
                val availability = biometricAuthManager.isBiometricAvailable()
                binding.textHardwareStatus.text = if (availability.isAvailable()) {
                    getString(R.string.biometric_hardware_available)
                } else {
                    getString(R.string.biometric_hardware_unavailable, availability.getErrorMessage())
                }

                // Enable/disable buttons based on status
                binding.buttonTestBiometric.isEnabled = status.isEnabled && availability.isAvailable()
                binding.buttonReenroll.isEnabled = status.isEnabled
                binding.switchBiometric.isEnabled = availability.isAvailable()
            }
            is BiometricSettingsUiState.Error -> {
                binding.progressBar.visibility = View.GONE
                binding.contentLayout.visibility = View.VISIBLE
                Toast.makeText(requireContext(), getLocalizedMessage(status.message), Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun enableBiometricAuthentication() {
        val availability = biometricAuthManager.isBiometricAvailable()
        
        if (!availability.isAvailable()) {
            Toast.makeText(
                requireContext(),
                availability.getErrorMessage() ?: getString(R.string.biometric_hardware_unavailable, "Unknown"),
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
                getString(R.string.biometric_login_required),
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
                getString(R.string.biometric_error_start, e.localizedMessage),
                Toast.LENGTH_LONG
            ).show()
            binding.switchBiometric.isChecked = false
            return
        }
        biometricPromptHelper.authenticateWithCrypto(
            cryptoObject = cryptoObject,
            title = getString(R.string.biometric_prompt_enable_title),
            subtitle = getString(R.string.biometric_prompt_enable_subtitle),
            description = getString(R.string.biometric_prompt_enable_desc),
            onSuccess = { result ->
                val cipher = result.cryptoObject?.cipher
                if (cipher == null) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.biometric_error_crypto),
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
                Toast.makeText(requireContext(), getString(R.string.biometric_cancelled), Toast.LENGTH_SHORT).show()
                binding.switchBiometric.isChecked = false
            }
        )
    }

    private fun disableBiometricAuthentication() {
        viewModel.disableBiometric()
    }

    private fun testBiometricAuthentication() {
        biometricPromptHelper.authenticate(
            title = getString(R.string.biometric_prompt_test_title),
            subtitle = getString(R.string.biometric_prompt_test_subtitle),
            description = getString(R.string.biometric_prompt_test_desc),
            onSuccess = {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.biometric_test_success),
                    Toast.LENGTH_SHORT
                ).show()
            },
            onError = { errorMessage ->
                Toast.makeText(
                    requireContext(),
                    getString(R.string.biometric_error_prefix, errorMessage),
                    Toast.LENGTH_LONG
                ).show()
            },
            onCancelled = {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.biometric_cancelled),
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
            getString(R.string.biometric_reenroll_instruction),
            Toast.LENGTH_LONG
        ).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
