package com.prog7314.IdleLoadsheddingMobile.ui.login

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
import com.prog7314.IdleLoadsheddingMobile.ui.register.StrataRegister
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class StrataLogin : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()  // (Firebase, 2025.)
    private lateinit var googleSignInHelper: GoogleSignInHelper  // (Firebase, 2025.)
    // (Firebase, 2025.)
    public override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        if (currentUser != null) {
            updateUI(currentUser)
        }
    }
    // (Firebase, 2025.)
    private fun showError(message: String) {
        Toast.makeText(baseContext, message, Toast.LENGTH_LONG).show()
    }
    // (Firebase, 2025.)
    private fun updateUI(user: FirebaseUser?) {
        if (user != null) {
            Log.d(TAG, "User ${user.uid} authenticated; pending UI integration.")
            // TODO: Connect this flow to the main app experience when the UI is ready.
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_strata_login)

        googleSignInHelper = GoogleSignInHelper(this)

        val editTextEmail = findViewById<TextView>(R.id.editTextEmail)
        val editTextPassword = findViewById<TextView>(R.id.editTextPassword)
        val buttonLogin = findViewById<Button>(R.id.buttonLogin)
        val buttonLoginGoogle = findViewById<Button>(R.id.buttonLoginGoogle)
        val textViewRegisterLink = findViewById<TextView>(R.id.textViewRegisterLink)

        textViewRegisterLink.setOnClickListener {
            val intent = Intent(this, StrataRegister::class.java)
            startActivity(intent)
        }
        // (Firebase, 2025.)
        buttonLoginGoogle.setOnClickListener {
            googleSignInHelper.initiateSignIn(
                activity = this,
                onSuccess = { user ->
                    Log.d(TAG, "Google Sign-In Success from Helper for user: ${user.uid}")
                    updateUI(user)
                },
                onFailure = { errorMessage ->
                    Log.e(TAG, "Google Sign-In Failure from Helper: $errorMessage")
                    showError(errorMessage)
                }
            )
        }
        // (Firebase, 2025.)
        buttonLogin.setOnClickListener {
            val email = editTextEmail.text.toString().trim()
            val password = editTextPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                showError("Please fill in all fields")
                return@setOnClickListener
            }
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        Log.d(TAG, "signInWithEmail:success")
                        updateUI(auth.currentUser)
                    } else {
                        Log.w(TAG, "signInWithEmail:failure", task.exception)
                        showError("Authentication failed: ${task.exception?.message}")
                    }
                }
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}
//Firebase, 2025. Firebase Authentication with Identity Services. [online] Available at:
//https://firebase.google.com/docs/auth/android/google-signin [Accessed 25 Apr. 2025].
