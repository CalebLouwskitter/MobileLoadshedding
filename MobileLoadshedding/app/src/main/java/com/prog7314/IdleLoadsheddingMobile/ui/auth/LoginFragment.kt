package com.prog7314.IdleLoadsheddingMobile.ui.auth

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.prog7314.IdleLoadsheddingMobile.R
import com.prog7314.IdleLoadsheddingMobile.databinding.FragmentLoginBinding
import com.prog7314.IdleLoadsheddingMobile.ui.auth.Biometrics.BiometricAuthManager
import com.prog7314.IdleLoadsheddingMobile.ui.auth.Biometrics.BiometricPromptHelper
import kotlinx.coroutines.launch

class LoginFragment : Fragment(R.layout.fragment_login) {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LoginViewModel by viewModels()
    private lateinit var googleSignInHelper: GoogleSignInHelper
    private lateinit var biometricPromptHelper: BiometricPromptHelper
    private lateinit var biometricAuthManager: BiometricAuthManager

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentLoginBinding.bind(view)

        googleSignInHelper = GoogleSignInHelper(requireContext())
        biometricAuthManager = BiometricAuthManager(requireContext())
        biometricPromptHelper = BiometricPromptHelper(requireActivity(), biometricAuthManager)

        binding.buttonLogin.setOnClickListener { onLoginClicked() }
        binding.buttonContinueGoogle.setOnClickListener { onGoogleSignInClicked() }
        binding.buttonBiometric.setOnClickListener { onBiometricLoginClicked() }
        binding.textForgotPassword.setOnClickListener { onForgotPasswordClicked() }
        binding.checkboxRememberMe.setOnCheckedChangeListener { buttonView, isChecked ->
            if (buttonView.isPressed) {
                onRememberMeChanged(isChecked)
            }
        }

        observeViewModel()
        updateBiometricButtonVisibility()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.authenticationState.collect { state ->
                        when (state) {
                            is AuthenticationState.Success -> {
                                Toast.makeText(context, "Login successful!", Toast.LENGTH_SHORT).show()
                                (activity as? AuthActivity)?.onAuthenticationSuccess()
                            }
                            is AuthenticationState.Error -> {
                                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                            }
                            is AuthenticationState.BiometricSetupRequired -> {
                                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                            }
                            is AuthenticationState.Loading -> {
                                // TODO: Show a loading indicator
                            }
                            else -> {}
                        }
                    }
                }
                launch {
                    viewModel.rememberMe.collect { remember ->
                        if (binding.checkboxRememberMe.isChecked != remember) {
                            binding.checkboxRememberMe.isChecked = remember
                        }
                    }
                }
                launch {
                    viewModel.rememberedEmail.collect { savedEmail ->
                        val currentEmail = binding.editEmail.text?.toString().orEmpty()
                        if (savedEmail.isNotEmpty() && currentEmail.isEmpty()) {
                            binding.editEmail.setText(savedEmail)
                            binding.editEmail.setSelection(savedEmail.length)
                        }
                    }
                }
                launch {
                    viewModel.biometricEnabled.collect { enabled ->
                        updateBiometricButtonVisibility()
                    }
                }
            }
        }
    }

    private fun onLoginClicked() {
        val email = binding.editEmail.text?.toString().orEmpty()
        val password = binding.editPassword.text?.toString().orEmpty()
        viewModel.trackLoginAttempt(email)
        viewModel.signIn(email, password)
        
        // After successful login, offer to enable biometric if available and not already enabled
        if (biometricAuthManager.isBiometricAvailable().isAvailable() && !viewModel.biometricEnabled.value) {
            // Enable biometric for this account
            viewModel.enableBiometricAuth(email)
        }
    }

    private fun onBiometricLoginClicked() {
        val (available, errorMessage) = biometricPromptHelper.checkAvailability()
        
        if (!available) {
            Toast.makeText(context, errorMessage ?: "Biometric not available", Toast.LENGTH_LONG).show()
            return
        }

        biometricPromptHelper.authenticate(
            title = "Sign in to Mobile Loadshedding",
            subtitle = "Use your biometric credential",
            description = "Authenticate to access your account",
            onSuccess = {
                viewModel.signInWithBiometric()
            },
            onError = { errorMessage ->
                Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
            },
            onCancelled = {
                // User cancelled, do nothing
            }
        )
    }

    private fun updateBiometricButtonVisibility() {
        val biometricAvailable = biometricAuthManager.isBiometricAvailable().isAvailable()
        val biometricEnabled = viewModel.biometricEnabled.value
        
        // Show biometric button only if hardware is available and user has enrolled biometric auth
        binding.buttonBiometric.visibility = if (biometricAvailable && biometricEnabled) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    private fun onGoogleSignInClicked() {
        googleSignInHelper.initiateSignIn(
            activity = requireActivity(),
            onSuccess = { user ->
                viewModel.onExternalSignInSuccess(user.email)
            },
            onFailure = { errorMessage ->
                Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
            }
        )
    }

    private fun onForgotPasswordClicked() {
        val email = binding.editEmail.text?.toString().orEmpty()
        viewModel.requestPasswordReset(email)
    }

    private fun onRememberMeChanged(isChecked: Boolean) {
        viewModel.setRememberMe(isChecked)
    }
}
