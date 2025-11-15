package com.prog7314.IdleLoadsheddingMobile.ui.auth

import android.os.Bundle
import android.view.View
import android.widget.Toast
import android.util.Base64
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.prog7314.IdleLoadsheddingMobile.R
import com.prog7314.IdleLoadsheddingMobile.databinding.FragmentLoginBinding
import com.prog7314.IdleLoadsheddingMobile.ui.auth.Biometrics.BiometricAuthManager
import com.prog7314.IdleLoadsheddingMobile.ui.auth.Biometrics.BiometricAvailability
import com.prog7314.IdleLoadsheddingMobile.ui.auth.Biometrics.BiometricCryptoManager
import com.prog7314.IdleLoadsheddingMobile.ui.auth.Biometrics.BiometricPromptHelper
import com.prog7314.IdleLoadsheddingMobile.ui.auth.Biometrics.BiometricRepository
import kotlinx.coroutines.launch

class LoginFragment : Fragment(R.layout.fragment_login) {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LoginViewModel by viewModels()
    private lateinit var googleSignInHelper: GoogleSignInHelper
    private lateinit var biometricPromptHelper: BiometricPromptHelper
    private lateinit var biometricAuthManager: BiometricAuthManager
    private lateinit var biometricRepository: BiometricRepository
    private val biometricCryptoManager by lazy { BiometricCryptoManager() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentLoginBinding.bind(view)

        try {
            googleSignInHelper = GoogleSignInHelper(requireContext())
            biometricAuthManager = BiometricAuthManager(requireContext())
            biometricPromptHelper = BiometricPromptHelper(requireActivity(), biometricAuthManager)
            biometricRepository = BiometricRepository(requireContext())
        } catch (e: Exception) {
            // If initialization fails, show error and hide biometric button
            Toast.makeText(context, "Failed to initialize authentication: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            binding.buttonBiometric.visibility = View.GONE
            return
        }

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
        
        // After successful login, set up biometric if available and enabled
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.authenticationState.collect { state ->
                if (state is AuthenticationState.Success) {
                    setupBiometricAfterLogin(email, password)
                }
            }
        }
    }
    
    private fun setupBiometricAfterLogin(email: String, password: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            // Check if biometric is enabled in settings
            val biometricEnabled = biometricRepository.getBiometricStatus().isEnabled
            if (!biometricEnabled || password.isEmpty()) {
                return@launch
            }
            
            // Check hardware availability
            val availability = biometricAuthManager.isBiometricAvailable()
            if (!availability.isAvailable()) {
                return@launch
            }
            
            // Create crypto object and encrypt credentials with biometric prompt
            try {
                val cryptoObject = biometricCryptoManager.createEncryptionCryptoObject()
                
                // Prompt for biometric to encrypt the credentials
                biometricPromptHelper.authenticateWithCrypto(
                    cryptoObject = cryptoObject,
                    title = "Secure your credentials",
                    subtitle = "Authenticate to enable biometric login",
                    description = "Your password will be securely encrypted",
                    onSuccess = { result ->
                        val cipher = result.cryptoObject?.cipher
                        if (cipher != null) {
                            try {
                                // Create payload with password
                                val payload = org.json.JSONObject().apply {
                                    put("email", email)
                                    put("password", password)
                                    put("uid", com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "")
                                    put("generatedAt", System.currentTimeMillis())
                                }.toString()
                                
                                // Encrypt and store
                                val encryptedData = biometricCryptoManager.encryptData(payload, cipher)
                                viewLifecycleOwner.lifecycleScope.launch {
                                    biometricRepository.storeEncryptedToken(encryptedData)
                                }
                            } catch (e: Exception) {
                                android.util.Log.w("LoginFragment", "Failed to encrypt credentials", e)
                            }
                        }
                    },
                    onError = { _ ->
                        // Silent fail - user cancelled or error occurred
                    },
                    onCancelled = {
                        // Silent fail - user cancelled
                    }
                )
            } catch (e: Exception) {
                // Silent fail - biometric setup is optional
                android.util.Log.w("LoginFragment", "Failed to set up biometric after login", e)
            }
        }
    }

    private fun onBiometricLoginClicked() {
        try {
            // Check if fragment is still attached
            if (!isAdded || activity == null) {
                return
            }

            // Check if biometrics need to be enrolled first
            if (!::biometricAuthManager.isInitialized) {
                Toast.makeText(context, "Biometric authentication not initialized", Toast.LENGTH_SHORT).show()
                return
            }

            val availability = biometricAuthManager.isBiometricAvailable()
            val biometricEnabled = viewModel.biometricEnabled.value ?: false
            
            // Case 1: Need to set up biometrics on device
            if (availability is BiometricAvailability.NoneEnrolled) {
                showBiometricSetupDialog()
                return
            }
            
            // Case 2: Biometric available but not enabled for user - prompt to login first
            if (availability.isAvailable() && !biometricEnabled) {
                androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Enable Biometric Login")
                    .setMessage("To enable biometric login, please sign in with your email and password first. Biometric authentication will be automatically set up for your next login.")
                    .setPositiveButton("OK") { _, _ ->
                        // Focus on email field to guide user
                        binding.editEmail.requestFocus()
                    }
                    .show()
                return
            }

            // Case 3: Biometric enabled and available - proceed with biometric login
            viewLifecycleOwner.lifecycleScope.launch {
                val encryptedData = try {
                    biometricRepository.getEncryptedToken()
                } catch (ex: Exception) {
                    if (isAdded && context != null) {
                        Toast.makeText(context, "Unable to read biometric credential: ${ex.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
                    return@launch
                }
                if (encryptedData == null) {
                    if (isAdded && context != null) {
                        Toast.makeText(context, "No biometric credential stored. Please enable biometrics again.", Toast.LENGTH_LONG).show()
                    }
                    return@launch
                }

                val ivBytes = Base64.decode(encryptedData.iv, Base64.NO_WRAP)
                val cryptoObject = biometricCryptoManager.createDecryptionCryptoObject(ivBytes)

                biometricPromptHelper.authenticateWithCrypto(
                    cryptoObject = cryptoObject,
                    title = "Sign in to Mobile Loadshedding",
                    subtitle = "Use your biometric credential",
                    description = "Authenticate to access your account",
                    onSuccess = { result ->
                        val cipher = result.cryptoObject?.cipher
                        if (cipher == null) {
                            Toast.makeText(context, "Unable to process biometric credential", Toast.LENGTH_LONG).show()
                            return@authenticateWithCrypto
                        }
                        try {
                            val payload = biometricCryptoManager.decryptData(encryptedData, cipher)
                            viewModel.signInWithBiometricToken(payload)
                        } catch (ex: Exception) {
                            if (isAdded && context != null) {
                                Toast.makeText(context, "Failed to decrypt credential: ${ex.localizedMessage}", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    onError = { errorMessage ->
                        if (isAdded && context != null) {
                            Toast.makeText(context, errorMessage ?: "Biometric authentication failed", Toast.LENGTH_LONG).show()
                        }
                    },
                    onCancelled = {
                        // User cancelled, do nothing
                    }
                )
            }
        } catch (e: Exception) {
            if (isAdded && context != null) {
                Toast.makeText(context, "Biometric authentication failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun updateBiometricButtonVisibility() {
        try {
            if (!::biometricAuthManager.isInitialized) {
                binding.buttonBiometric.visibility = View.GONE
                return
            }

            val availability = biometricAuthManager.isBiometricAvailable()
            val biometricEnabled = viewModel.biometricEnabled.value ?: false
            
            android.util.Log.d("LoginFragment", "Biometric availability: $availability, enabled: $biometricEnabled")
            
            // Handle different availability states
            when {
                // Case 1: Biometric hardware available AND user has enabled it for their account
                availability.isAvailable() && biometricEnabled -> {
                    binding.buttonBiometric.visibility = View.VISIBLE
                    binding.buttonBiometric.text = "Sign in with Biometrics"
                    android.util.Log.d("LoginFragment", "Showing biometric login button (available and enabled)")
                }
                // Case 2: No biometric enrolled on device - show setup prompt
                availability is BiometricAvailability.NoneEnrolled -> {
                    binding.buttonBiometric.visibility = View.VISIBLE
                    binding.buttonBiometric.text = "Set up Biometric Login"
                    android.util.Log.d("LoginFragment", "Showing setup button (none enrolled)")
                }
                // Case 3: Biometric available but not enabled for this user - show enable option
                availability.isAvailable() && !biometricEnabled -> {
                    binding.buttonBiometric.visibility = View.VISIBLE
                    binding.buttonBiometric.text = "Enable Biometric Login"
                    android.util.Log.d("LoginFragment", "Showing enable button (available but not enabled)")
                }
                // Case 4: Biometric not available (no hardware, etc.)
                else -> {
                    binding.buttonBiometric.visibility = View.GONE
                    android.util.Log.d("LoginFragment", "Hiding biometric button - availability: ${availability::class.simpleName}, enabled: $biometricEnabled")
                }
            }
        } catch (e: Exception) {
            // If any error occurs, hide the button
            android.util.Log.e("LoginFragment", "Error updating biometric button", e)
            binding.buttonBiometric.visibility = View.GONE
        }
    }

    private fun showBiometricSetupDialog() {
        if (!isAdded || context == null) return
        
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Set Up Biometric Authentication")
            .setMessage("No biometric credentials are enrolled on this device.\n\nWould you like to set up fingerprint or face recognition now?\n\nYou can also use your device PIN, pattern, or password as an alternative.")
            .setPositiveButton("Go to Settings") { _, _ ->
                try {
                    val intent = android.content.Intent(android.provider.Settings.ACTION_BIOMETRIC_ENROLL)
                    intent.putExtra(
                        android.provider.Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
                        androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG or 
                        androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
                    )
                    startActivity(intent)
                } catch (e: Exception) {
                    // Fallback to general security settings if specific biometric settings not available
                    try {
                        val intent = android.content.Intent(android.provider.Settings.ACTION_SECURITY_SETTINGS)
                        startActivity(intent)
                    } catch (e2: Exception) {
                        Toast.makeText(context, "Unable to open settings: ${e2.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
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
