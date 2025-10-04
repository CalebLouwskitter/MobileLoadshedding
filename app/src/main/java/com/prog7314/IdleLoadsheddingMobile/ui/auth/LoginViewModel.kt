package com.prog7314.IdleLoadsheddingMobile.ui.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.prog7314.IdleLoadsheddingMobile.auth.AuthPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LoginViewModel(application: Application) : AndroidViewModel(application) {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val authPreferences = AuthPreferences(application)

    private val _authenticationState = MutableStateFlow<AuthenticationState>(AuthenticationState.Idle)
    val authenticationState = _authenticationState.asStateFlow()

    val rememberMe = authPreferences.rememberMe.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = false
    )

    val rememberedEmail = authPreferences.rememberedEmail.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = ""
    )

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
                        viewModelScope.launch {
                            if (rememberMe.value) {
                                authPreferences.setRememberedEmail(email)
                            } else {
                                authPreferences.clearRememberedEmail()
                            }
                        }
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
        viewModelScope.launch {
            authPreferences.setRememberMe(remember)
        }
    }

    fun onExternalSignInSuccess(email: String?) {
        viewModelScope.launch {
            if (rememberMe.value && !email.isNullOrBlank()) {
                authPreferences.setRememberedEmail(email)
            } else if (!rememberMe.value) {
                authPreferences.clearRememberedEmail()
            }
        }
        _authenticationState.value = AuthenticationState.Success
    }
}

sealed class AuthenticationState {
    object Idle : AuthenticationState()
    object Loading : AuthenticationState()
    object Success : AuthenticationState()
    data class Error(val message: String) : AuthenticationState()
}
