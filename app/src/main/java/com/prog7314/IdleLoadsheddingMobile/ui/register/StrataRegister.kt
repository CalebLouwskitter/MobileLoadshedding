package com.prog7314.IdleLoadsheddingMobile.ui.register

import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.prog7314.IdleLoadsheddingMobile.R
import com.prog7314.IdleLoadsheddingMobile.ui.auth.GoogleSignInHelper
import com.prog7314.IdleLoadsheddingMobile.ui.login.StrataLogin
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser

class StrataRegister : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInHelper: GoogleSignInHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_strata_register)

        auth = FirebaseAuth.getInstance()  // (Firebase, 2025.)
        googleSignInHelper = GoogleSignInHelper(this)

        val editTextEmail = findViewById<TextView>(R.id.editTextEmail)
        val editTextPassword = findViewById<TextView>(R.id.editTextPassword)
        val editTextConfirmPassword = findViewById<TextView>(R.id.editTextConfirmPassword)
        val buttonReg = findViewById<Button>(R.id.buttonRegister)
        val buttonRegGoogle = findViewById<Button>(R.id.buttonRegisterGoogle)
        val textViewLoginLink = findViewById<TextView>(R.id.textViewLoginLink)

        textViewLoginLink.setOnClickListener {
            val intent = Intent(this, StrataLogin::class.java)
            startActivity(intent)
            finish()
        }
        // (Firebase, 2025.)
        buttonRegGoogle.setOnClickListener {
            googleSignInHelper.initiateSignIn(
                activity = this,
                onSuccess = { user ->
                    Log.d(TAG, "Google Sign-Up/In Success from Helper for user: ${user.uid}")
                    showToast("Google Sign-In Successful!")
                    updateUI(user)
                },
                onFailure = { errorMessage ->
                    Log.e(TAG, "Google Sign-In Failure from Helper: $errorMessage")
                    showError(errorMessage)
                }
            )
        }
        // (Firebase, 2025.)
        buttonReg.setOnClickListener {
            val email = editTextEmail.text.toString().trim()
            val password = editTextPassword.text.toString().trim()
            val confirmPassword = editTextConfirmPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                showError("Please fill in all fields")
                return@setOnClickListener
            }
            if (password != confirmPassword) {
                showError("Passwords do not match")
                return@setOnClickListener
            }
            auth.createUserWithEmailAndPassword(email, password)  // (Firebase, 2025.)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        Log.d(TAG, "createUserWithEmail:success")
                        showToast("Registration successful!")
                        updateUI(auth.currentUser)
                    } else {
                        Log.w(TAG, "createUserWithEmail:failure", task.exception)  // (Firebase, 2025.)
                        val message = when (task.exception) {
                            is FirebaseAuthWeakPasswordException -> "Password is too weak."
                            is FirebaseAuthInvalidCredentialsException -> "Invalid email format."
                            is FirebaseAuthUserCollisionException -> "Email address is already registered."
                            else -> task.exception?.localizedMessage ?: "Registration failed."
                        }
                        showError(message)
                    }
                }
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun updateUI(user: FirebaseUser?) {
        if (user != null) {
            Log.d(TAG, "User ${user.uid} registered; pending UI integration.")
            // TODO: Connect this flow to the main app experience when the UI is ready.
        }
    }
}

//Firebase, 2025. Firebase Authentication with Identity Services. [online] Available at:
//https://firebase.google.com/docs/auth/android/google-signin [Accessed 25 Apr. 2025].
