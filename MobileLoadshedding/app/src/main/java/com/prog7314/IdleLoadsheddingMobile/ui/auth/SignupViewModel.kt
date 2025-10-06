package com.prog7314.IdleLoadsheddingMobile.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SignupViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val _registrationState = MutableStateFlow<RegistrationState>(RegistrationState.Idle)
    val registrationState = _registrationState.asStateFlow()

    fun createAccount(email: String, password: String, confirmPassword: String) {
        if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            _registrationState.value = RegistrationState.Error("Please fill in all fields.")
            return
        }
        if (password != confirmPassword) {
            _registrationState.value = RegistrationState.Error("Passwords do not match.")
            return
        }

        viewModelScope.launch {
            _registrationState.value = RegistrationState.Loading
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        _registrationState.value = RegistrationState.Success
                    } else {
                        val message = when (task.exception) {
                            is FirebaseAuthWeakPasswordException -> "Password is too weak."
                            is FirebaseAuthInvalidCredentialsException -> "Invalid email format."
                            is FirebaseAuthUserCollisionException -> "Email address is already registered."
                            else -> task.exception?.localizedMessage ?: "Registration failed."
                        }
                        _registrationState.value = RegistrationState.Error(message)
                    }
                }
        }
    }

    fun setRememberMe(remember: Boolean) {
        // TODO: Persist remember-me preference for newly registered accounts.
    }
}

sealed class RegistrationState {
    object Idle : RegistrationState()
    object Loading : RegistrationState()
    object Success : RegistrationState()
    data class Error(val message: String) : RegistrationState()
}
