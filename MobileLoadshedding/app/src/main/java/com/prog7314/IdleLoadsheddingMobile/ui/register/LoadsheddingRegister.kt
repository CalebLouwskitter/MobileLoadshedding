package com.prog7314.IdleLoadsheddingMobile.ui.register

import android.content.Intent //Android, 2025.
import android.os.Bundle //Android, 2025.
import androidx.appcompat.app.AppCompatActivity //AndroidX, 2025.
import com.prog7314.IdleLoadsheddingMobile.ui.auth.AuthActivity

/**
 * LoadsheddingRegister redirects users to the authentication activity
 * with signup as the starting destination.
 */
class LoadsheddingRegister : AppCompatActivity() { //AndroidX, 2025.

    override fun onCreate(savedInstanceState: Bundle?) { //Android, 2025.
        super.onCreate(savedInstanceState)
        val intent = Intent(this, AuthActivity::class.java).apply { //Android, 2025.
            putExtra(AuthActivity.EXTRA_START_DESTINATION, AuthActivity.DESTINATION_SIGNUP) //Android, 2025.
        }
        startActivity(intent) //Android, 2025.
        finish() //Android, 2025.
    }
}

/**
 * Reference List:
 * - Android, 2025. Android Developers - Intent. [online] Available at:
 *   https://developer.android.com/reference/android/content/Intent [Accessed 1 Oct. 2025].
 * - AndroidX, 2025. AndroidX AppCompat Library. [online] Available at:
 *   https://developer.android.com/jetpack/androidx/releases/appcompat [Accessed 1 Oct. 2025].
 */
