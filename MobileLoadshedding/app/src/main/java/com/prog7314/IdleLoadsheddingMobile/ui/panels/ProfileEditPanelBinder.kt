package com.prog7314.IdleLoadsheddingMobile.ui.panels

import android.app.Activity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.prog7314.IdleLoadsheddingMobile.R

/**
 * Handles profile edit panel configuration.
 * Extracted from MainActivity to separate concerns.
 */
class ProfileEditPanelBinder(
    private val activity: Activity,
    private val auth: FirebaseAuth
) {
    
    /**
     * Configures profile edit panel UI elements and event handlers.
     */
    fun configure(
        panel: View,
        onShowPanel: (Int) -> Unit
    ) {
        val user = auth.currentUser
        
        // Populate current nickname and email
        panel.findViewById<EditText?>(R.id.input_nickname)?.setText(user?.displayName ?: "")
        panel.findViewById<TextView?>(R.id.text_email_static)?.text = user?.email ?: "Not logged in"
        
        // Cancel button - return to profile
        panel.findViewById<Button?>(R.id.btn_cancel_profile)?.setOnClickListener {
            onShowPanel(R.layout.panel_profile)
        }

        // Save button - update Firebase profile
        panel.findViewById<Button?>(R.id.btn_save_profile)?.setOnClickListener {
            val nicknameInput = panel.findViewById<EditText?>(R.id.input_nickname)
            val newNickname = nicknameInput?.text?.toString()?.trim()
            
            if (!newNickname.isNullOrEmpty()) {
                // Update Firebase user profile
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(newNickname)
                    .build()
                
                user?.updateProfile(profileUpdates)?.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(activity, activity.getString(R.string.toast_profile_updated), Toast.LENGTH_SHORT).show()
                        onShowPanel(R.layout.panel_profile)
                    } else {
                        Toast.makeText(activity, activity.getString(R.string.toast_profile_update_failed), Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(activity, activity.getString(R.string.toast_nickname_empty), Toast.LENGTH_SHORT).show()
            }
        }
    }
}
