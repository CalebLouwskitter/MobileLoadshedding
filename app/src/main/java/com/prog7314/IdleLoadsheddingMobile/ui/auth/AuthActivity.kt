package com.prog7314.IdleLoadsheddingMobile.ui.auth

import android.content.Intent //Android, 2025.
import android.os.Bundle //Android, 2025.
import androidx.activity.enableEdgeToEdge //AndroidX, 2025.
import androidx.appcompat.app.AppCompatActivity //AndroidX, 2025.
import androidx.core.view.ViewCompat //AndroidX, 2025.
import androidx.core.view.WindowInsetsCompat //AndroidX, 2025.
import androidx.navigation.NavController //AndroidX, 2025.
import androidx.navigation.fragment.NavHostFragment //AndroidX, 2025.
import com.google.android.material.button.MaterialButtonToggleGroup //Material Design, 2025.
import com.prog7314.IdleLoadsheddingMobile.MainActivity
import com.prog7314.IdleLoadsheddingMobile.R
import com.prog7314.IdleLoadsheddingMobile.databinding.ActivityAuthBinding

/**
 * AuthActivity manages the authentication flow, including login and signup screens.
 */
class AuthActivity : AppCompatActivity() { //AndroidX, 2025.

    companion object {
        const val EXTRA_START_DESTINATION = "extra_start_destination"
        const val DESTINATION_LOGIN = "login"
        const val DESTINATION_SIGNUP = "signup"
    }

    private lateinit var binding: ActivityAuthBinding //AndroidX, 2025.
    private lateinit var navController: NavController //AndroidX, 2025.

    private val destinationListener = NavController.OnDestinationChangedListener { _, destination, _ -> //AndroidX, 2025.
        when (destination.id) {
            R.id.loginFragment -> synchronizeToggleSelection(R.id.button_toggle_login)
            R.id.signupFragment -> synchronizeToggleSelection(R.id.button_toggle_signup)
        }
    }

    private val toggleListener = MaterialButtonToggleGroup.OnButtonCheckedListener { _, checkedId, isChecked -> //Material Design, 2025.
        if (!isChecked) return@OnButtonCheckedListener
        when (checkedId) {
            R.id.button_toggle_login -> navigateIfNeeded(R.id.loginFragment)
            R.id.button_toggle_signup -> navigateIfNeeded(R.id.signupFragment)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) { //Android, 2025.
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() //AndroidX, 2025.
        binding = ActivityAuthBinding.inflate(layoutInflater) //AndroidX, 2025.
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets -> //AndroidX, 2025.
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars()) //AndroidX, 2025.
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.auth_nav_host_fragment) as NavHostFragment //AndroidX, 2025.
        navController = navHostFragment.navController //AndroidX, 2025.
        navController.addOnDestinationChangedListener(destinationListener) //AndroidX, 2025.

        binding.toggleAuth.addOnButtonCheckedListener(toggleListener) //Material Design, 2025.

        if (savedInstanceState == null) {
            val initialButtonId = when (intent.getStringExtra(EXTRA_START_DESTINATION)) { //Android, 2025.
                DESTINATION_SIGNUP -> R.id.button_toggle_signup
                else -> R.id.button_toggle_login
            }
            binding.toggleAuth.check(initialButtonId) //Material Design, 2025.
        } else {
            synchronizeToggleSelection(
                when (navController.currentDestination?.id) { //AndroidX, 2025.
                    R.id.signupFragment -> R.id.button_toggle_signup
                    else -> R.id.button_toggle_login
                }
            )
        }
    }

    override fun onDestroy() { //Android, 2025.
        navController.removeOnDestinationChangedListener(destinationListener) //AndroidX, 2025.
        binding.toggleAuth.removeOnButtonCheckedListener(toggleListener) //Material Design, 2025.
        super.onDestroy()
    }

    private fun navigateIfNeeded(destinationId: Int) {
        if (navController.currentDestination?.id == destinationId) return //AndroidX, 2025.
        navController.navigate(destinationId) //AndroidX, 2025.
    }

    private fun synchronizeToggleSelection(buttonId: Int) {
        if (binding.toggleAuth.checkedButtonId == buttonId) return //Material Design, 2025.
        binding.toggleAuth.check(buttonId) //Material Design, 2025.
    }

    fun onAuthenticationSuccess() {
        val intent = Intent(this, MainActivity::class.java) //Android, 2025.
        startActivity(intent) //Android, 2025.
        finish() //Android, 2025.
    }
}

/**
 * Reference List:
 * - Android, 2025. Android Developers Documentation. [online] Available at:
 *   https://developer.android.com/ [Accessed 1 Oct. 2025].
 * - AndroidX, 2025. AndroidX Navigation Component. [online] Available at:
 *   https://developer.android.com/guide/navigation [Accessed 1 Oct. 2025].
 * - Material Design, 2025. Material Components for Android. [online] Available at:
 *   https://material.io/develop/android [Accessed 1 Oct. 2025].
 */
