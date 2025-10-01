package com.prog7314.IdleLoadsheddingMobile.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LoginViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val _authenticationState = MutableStateFlow<AuthenticationState>(AuthenticationState.Idle)
    val authenticationState = _authenticationState.asStateFlow()

    fun signIn(email: String, password: String) {
        if (email.isEmpty() || password.isEmpty()) {
            _authenticationState.value = AuthenticationState.Error("Please fill in all fields.")
            return
        }

        viewModelScope.launch {
            _authenticationState.value = AuthenticationState.Loading
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        _authenticationState.value = AuthenticationState.Success
                    } else {
                        _authenticationState.value = AuthenticationState.Error(
                            task.exception?.localizedMessage ?: "Authentication failed."
                        )
                    }
                }
        }
    }

    fun trackLoginAttempt(email: String) {
        // TODO: Replace placeholder with analytics or logging once available.
    }

    fun requestPasswordReset(email: String) {
        if (email.isEmpty()) {
            _authenticationState.value = AuthenticationState.Error("Please enter your email address.")
            return
        }
        viewModelScope.launch {
            auth.sendPasswordResetEmail(email)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // Optionally, you can emit a different state for password reset success
                    } else {
                        _authenticationState.value = AuthenticationState.Error(
                            task.exception?.localizedMessage ?: "Password reset failed."
                        )
                    }
                }
        }
    }

    fun setRememberMe(remember: Boolean) {
        // TODO: Persist remember-me preference when local storage layer is available.
    }
}

sealed class AuthenticationState {
    object Idle : AuthenticationState()
    object Loading : AuthenticationState()
    object Success : AuthenticationState()
    data class Error(val message: String) : AuthenticationState()
}
