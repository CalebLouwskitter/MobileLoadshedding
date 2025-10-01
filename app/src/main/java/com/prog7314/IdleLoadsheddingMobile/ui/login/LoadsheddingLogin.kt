package com.prog7314.IdleLoadsheddingMobile.ui.login

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.prog7314.IdleLoadsheddingMobile.ui.auth.AuthActivity

class LoadsheddingLogin : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = Intent(this, AuthActivity::class.java).apply {
            putExtra(AuthActivity.EXTRA_START_DESTINATION, AuthActivity.DESTINATION_LOGIN)
        }
        startActivity(intent)
        finish()
    }
}
