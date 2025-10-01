package com.prog7314.IdleLoadsheddingMobile.ui.auth

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.prog7314.IdleLoadsheddingMobile.R
import com.prog7314.IdleLoadsheddingMobile.databinding.FragmentSignupBinding
import kotlinx.coroutines.launch

class SignupFragment : Fragment(R.layout.fragment_signup) {

    private var _binding: FragmentSignupBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SignupViewModel by viewModels()
    private lateinit var googleSignInHelper: GoogleSignInHelper

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSignupBinding.bind(view)

        googleSignInHelper = GoogleSignInHelper(requireContext())

        binding.buttonSignup.setOnClickListener { onSignupClicked() }
        binding.buttonSignupGoogle.setOnClickListener { onGoogleSignupClicked() }
        binding.checkboxSignupRemember.setOnCheckedChangeListener { _, isChecked -> onRememberMeChanged(isChecked) }
        binding.textAlreadySigned.setOnClickListener { onAlreadySignedUp() }

        observeViewModel()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.registrationState.collect { state ->
                when (state) {
                    is RegistrationState.Success -> {
                        Toast.makeText(context, "Registration successful!", Toast.LENGTH_SHORT).show()
                        (activity as? AuthActivity)?.onAuthenticationSuccess()
                    }
                    is RegistrationState.Error -> {
                        Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                    }
                    is RegistrationState.Loading -> {
                        // TODO: Show a loading indicator
                    }
                    else -> {}
                }
            }
        }
    }

    private fun onSignupClicked() {
        val email = binding.editSignupEmail.text?.toString().orEmpty()
        val password = binding.editSignupPassword.text?.toString().orEmpty()
        val confirm = binding.editSignupConfirm.text?.toString().orEmpty()
        viewModel.createAccount(email, password, confirm)
    }

    private fun onGoogleSignupClicked() {
        googleSignInHelper.initiateSignIn(
            activity = requireActivity(),
            onSuccess = {
                Toast.makeText(context, "Google Sign-In Successful!", Toast.LENGTH_SHORT).show()
                (activity as? AuthActivity)?.onAuthenticationSuccess()
            },
            onFailure = { errorMessage ->
                Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
            }
        )
    }

    private fun onRememberMeChanged(isChecked: Boolean) {
        viewModel.setRememberMe(isChecked)
    }

    private fun onAlreadySignedUp() {
        findNavController().navigate(R.id.loginFragment)
    }
}
