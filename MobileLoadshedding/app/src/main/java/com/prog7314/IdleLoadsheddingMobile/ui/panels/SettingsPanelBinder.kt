package com.prog7314.IdleLoadsheddingMobile.ui.panels

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.firebase.auth.FirebaseAuth
import com.prog7314.IdleLoadsheddingMobile.R
import com.prog7314.IdleLoadsheddingMobile.data.models.UnityEventPayload
import com.prog7314.IdleLoadsheddingMobile.data.models.UnityLeaderboardPayload
import com.prog7314.IdleLoadsheddingMobile.data.models.UnitySavePayload
import com.prog7314.IdleLoadsheddingMobile.data.models.UnityStatsSnapshot
import com.prog7314.IdleLoadsheddingMobile.unity.SaveSync
import com.prog7314.IdleLoadsheddingMobile.unity.Stats
import com.prog7314.IdleLoadsheddingMobile.utils.FormatUtils
import com.prog7314.IdleLoadsheddingMobile.utils.LocaleManager

/**
 * Handles settings panel configuration including save sync, language selection, and logout.
 * Extracted from MainActivity to separate concerns.
 */
class SettingsPanelBinder(
    private val activity: Activity,
    private val auth: FirebaseAuth,
    private val settingsPrefs: SharedPreferences,
    private val formatUtils: FormatUtils,
    private val localeManager: LocaleManager,
    private val onClearGameState: () -> Unit
) {
    
    companion object {
        private const val KEY_LANGUAGE_INDEX = "language_index"
        private const val DEFAULT_LANGUAGE_INDEX = 0
    }
    
    /**
     * Configures settings panel UI elements and event handlers.
     */
    fun configure(
        panel: View,
        save: UnitySavePayload?
    ) {
        // Upload save button
        panel.findViewById<Button?>(R.id.btn_upload_save)?.setOnClickListener {
            SaveSync.requestUnitySave()
            Toast.makeText(activity, activity.getString(R.string.toast_save_uploaded), Toast.LENGTH_SHORT).show()
        }

        // Sync save button
        panel.findViewById<Button?>(R.id.btn_sync_save)?.setOnClickListener {
            Stats.requestUnitySnapshot()
            SaveSync.requestUnitySave()
            Toast.makeText(activity, activity.getString(R.string.toast_save_synced), Toast.LENGTH_SHORT).show()
        }

        // Display last sync time
        val lastSyncText = panel.findViewById<TextView?>(R.id.text_last_save_sync)
        save?.let {
            lastSyncText?.text = "Last synced: ${formatUtils.formatTimestamp(it.timestampUtc)}"
        }
        
        // Language spinner setup
        configureLanguageSpinner(panel)
        
        // Theme spinner—wired up but not yet functional
        val themeSpinner = panel.findViewById<Spinner?>(R.id.spinner_theme)
        themeSpinner?.setSelection(0)  // Always default to Light for now

        // Logout button
        panel.findViewById<Button?>(R.id.btn_logout)?.setOnClickListener {
            showLogoutConfirmationDialog()
        }

        // Back arrow in header - behaves like closing settings and returning to profile
        panel.findViewById<View?>(R.id.btn_back_settings)?.setOnClickListener {
            // Profile panel is responsible for showing settings, so we navigate back to it
            activity.runOnUiThread {
                // MainActivity uses showPanel(R.layout.panel_profile) for profile
                (activity as? com.prog7314.IdleLoadsheddingMobile.MainActivity)?.let { main ->
                    main.runOnUiThread {
                        main.showPanel(com.prog7314.IdleLoadsheddingMobile.R.layout.panel_profile)
                    }
                }
            }
        }
    }
    
    /**
     * Configures the language selection spinner with proper state management.
     * Handles the tricky issue of initial selection firing onItemSelected.
     */
    private fun configureLanguageSpinner(panel: View) {
        val languageSpinner = panel.findViewById<Spinner?>(R.id.spinner_language) ?: return
        
        // Restore previously saved language selection
        val savedIndex = settingsPrefs.getInt(KEY_LANGUAGE_INDEX, DEFAULT_LANGUAGE_INDEX)
        
        // Validate saved index to prevent crashes
        val languageCount = activity.resources.getStringArray(R.array.settings_languages).size
        val safeIndex = savedIndex.coerceIn(0, languageCount - 1)
        
        // Flag to ignore the initial onItemSelected callback
        var isInitialSelection = true
        
        languageSpinner.setSelection(safeIndex)
        
        languageSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                // Skip the callback that fires when we programmatically set selection
                if (isInitialSelection) {
                    isInitialSelection = false
                    return
                }
                
                // User actually changed the language—persist and apply it
                if (position != savedIndex) {
                    Log.d("SettingsPanelBinder", "Language changed from index $savedIndex to $position")
                    
                    // Save preference immediately
                    settingsPrefs.edit().putInt(KEY_LANGUAGE_INDEX, position).apply()
                    
                    // Apply the new locale
                    localeManager.applyLocale(position)
                    
                    // Show feedback toast BEFORE recreation
                    Toast.makeText(
                        activity,
                        activity.getString(R.string.settings_language_changed),
                        Toast.LENGTH_LONG
                    ).show()
                    
                    // Recreate activity to apply locale change to all views
                    activity.recreate()
                }
            }
            
            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Required by interface but not used
            }
        }
    }
    
    /**
     * Shows a confirmation dialog before logging out.
     */
    private fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(activity)
            .setTitle(activity.getString(R.string.logout_confirmation_title))
            .setMessage(activity.getString(R.string.logout_confirmation_message))
            .setPositiveButton(activity.getString(R.string.logout_confirm_yes)) { _, _ ->
                performLogout()
            }
            .setNegativeButton(activity.getString(R.string.logout_confirm_no), null)
            .show()
    }
    
    /**
     * Performs the logout sequence:
     * 1. Upload final save to cloud
     * 2. Sign out from Firebase
     * 3. Clear cached data
     * 4. Navigate to AuthActivity
     * 5. Finish MainActivity
     */
    private fun performLogout() {
        // Upload save one last time to ensure progress is preserved
        SaveSync.requestUnitySave()

        // Sign out from Firebase
        auth.signOut()

        // Clear cached game state via callback
        onClearGameState()

        // Show success feedback
        Toast.makeText(activity, activity.getString(R.string.toast_logout_success), Toast.LENGTH_SHORT).show()

        // Navigate to AuthActivity
        val intent = Intent(activity, com.prog7314.IdleLoadsheddingMobile.ui.auth.AuthActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        activity.startActivity(intent)

        // Finish this activity so user can't navigate back with back button
        activity.finish()
    }
}
