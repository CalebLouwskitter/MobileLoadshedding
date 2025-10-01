package com.prog7314.IdleLoadsheddingMobile.ui.auth

import android.app.Activity //Android, 2025.
import android.content.ContentValues.TAG //Android, 2025.
import android.content.Context //Android, 2025.
import android.util.Log //Android, 2025.
import androidx.credentials.Credential //AndroidX, 2025.
import androidx.credentials.CredentialManager //AndroidX, 2025.
import androidx.credentials.CustomCredential //AndroidX, 2025.
import androidx.credentials.GetCredentialRequest //AndroidX, 2025.
import androidx.credentials.exceptions.GetCredentialException //AndroidX, 2025.
import com.prog7314.IdleLoadsheddingMobile.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption //Google, 2025.
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential //Google, 2025.
import com.google.firebase.auth.FirebaseAuth //Firebase, 2025.
import com.google.firebase.auth.FirebaseUser //Firebase, 2025.
import com.google.firebase.auth.GoogleAuthProvider //Firebase, 2025.
import com.prog7314.IdleLoadsheddingMobile.networking.FirebaseIdTokenProvider
import kotlinx.coroutines.CoroutineScope //Kotlin Coroutines, 2025.
import kotlinx.coroutines.Dispatchers //Kotlin Coroutines, 2025.
import kotlinx.coroutines.launch //Kotlin Coroutines, 2025.

/**
 * GoogleSignInHelper handles the process of initiating and authenticating users using Google Sign-In,
 * integrating with Firebase Authentication.
 */

class GoogleSignInHelper(private val context: Context) { //Android, 2025.
    // CredentialManager is part of the Android Identity Services API
    private val credentialManager = CredentialManager.create(context) //AndroidX, 2025.
    // Firebase Authentication instance
    private val auth = FirebaseAuth.getInstance() //Firebase, 2025.


    // Builds the Google ID token option using the server client ID
    private val googleIdOption: GetGoogleIdOption by lazy { //Google, 2025.
        GetGoogleIdOption.Builder() //Google, 2025.
            .setServerClientId(context.getString(R.string.default_web_client_id)) //Google, 2025.
            .setFilterByAuthorizedAccounts(false) //Google, 2025.
            .build()
    }
    // Credential request configuration for Google Sign-In
    private val request: GetCredentialRequest by lazy {
        GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()
    }

    /**
     * Initiates Google Sign-In by showing a UI prompt to the user and handling the result.
     *
     * @param activity The activity context used for launching UI prompts.
     * @param onSuccess Callback with FirebaseUser if sign-in is successful.
     * @param onFailure Callback with error message if sign-in fails.
     */

    fun initiateSignIn(  //Google, 2025.
        activity: Activity,
        onSuccess: (FirebaseUser) -> Unit,
        onFailure: (String) -> Unit
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val result = credentialManager.getCredential(
                    context = activity,
                    request = request
                )
                handleSignIn(result.credential, onSuccess, onFailure)
            } catch (e: GetCredentialException) {
                Log.e(TAG, "GetCredentialException: ${e.message}", e)
                onFailure("Google Sign-In Failed: ${e.message}")
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error during getCredential: ${e.message}", e)
                onFailure("An unexpected error occurred during Google Sign-In.")
            }
        }
    }

    /**
     * Processes the returned Credential and determines how to handle it.
     *
     * @param credential The returned credential from Google Sign-In.
     * @param onSuccess Callback with FirebaseUser if sign-in is successful.
     * @param onFailure Callback with error message if sign-in fails.
     */

    private fun handleSignIn(  //Google, 2025.
        credential: Credential,
        onSuccess: (FirebaseUser) -> Unit,
        onFailure: (String) -> Unit
    ) {
        if (credential is GoogleIdTokenCredential) { //Google, 2025.
            firebaseAuthWithGoogle(credential.idToken, onSuccess, onFailure)
        } else if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            try {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                firebaseAuthWithGoogle(googleIdTokenCredential.idToken, onSuccess, onFailure)
            } catch (e: Exception) {
                Log.e(TAG, "Error creating GoogleIdTokenCredential from CustomCredential: ${e.message}", e) //Google, 2025.
                onFailure("Google Sign-In Error: Could not process credential data.")
            }
        } else {
            Log.w(TAG, "Unexpected credential type: ${credential::class.java.name}") //Google, 2025.
            onFailure("Google Sign-In Error: Unexpected credential type.")
        }
    }

    /**
     * Authenticates with Firebase using a Google ID Token credential.
     *
     * @param idToken The Google ID token retrieved from the sign-in result.
     * @param onSuccess Callback with FirebaseUser if authentication is successful.
     * @param onFailure Callback with error message if authentication fails.
     */

    private fun firebaseAuthWithGoogle( //Firebase, 2025.
        idToken: String,
        onSuccess: (FirebaseUser) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)  //Google, 2025.
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "signInWithCredential:success")
                    auth.currentUser?.let { user ->
                        CoroutineScope(Dispatchers.IO).launch {
                            FirebaseIdTokenProvider.getToken(forceRefresh = true)
                        }
                        onSuccess(user)
                    } ?: onFailure("Firebase Authentication succeeded but user was null.") //Google, 2025.
                } else {
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                    onFailure("Firebase Authentication Failed: ${task.exception?.message}")
                }
            }
    }
}

/**
 * Reference List:
 * - Google, 2025. Authenticate with Firebase using ID tokens. [online] Available at:
 *   https://developers.google.com/identity/gsi/web/guides/firebase-auth [Accessed 25 September. 2025].
 * - Firebase, 2025. Authenticate with Google on Android. [online] Available at:
 *   https://firebase.google.com/docs/auth/android/google-signin [Accessed 1 Oct. 2025].
 * - Google, 2025. Sign in with Google for Android - Credential Manager. [online] Available at:
 *   https://developers.google.com/identity/android-credential-manager [Accessed 1 Oct. 2025].
 * - Android, 2025. Credential Manager. [online] Available at:
 *   https://developer.android.com/jetpack/androidx/releases/credentials [Accessed 1 Oct. 2025].
 * - Kotlin Coroutines, 2025. Kotlin Coroutines Guide. [online] Available at:
 *   https://kotlinlang.org/docs/coroutines-guide.html [Accessed 1 Oct. 2025].
 */
