package com.prog7314.IdleLoadsheddingMobile.ui.panels

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.prog7314.IdleLoadsheddingMobile.R
import com.prog7314.IdleLoadsheddingMobile.data.models.UnitySavePayload
import com.prog7314.IdleLoadsheddingMobile.data.models.UnityStatsSnapshot
import com.prog7314.IdleLoadsheddingMobile.utils.FormatUtils

/**
 * Handles profile panel configuration and data binding.
 * Extracted from MainActivity to separate concerns.
 */
class ProfilePanelBinder(
    private val context: Context,
    private val formatUtils: FormatUtils,
    private val auth: FirebaseAuth
) {
    
    /**
     * Configures profile panel UI elements and event handlers.
     */
    fun configure(
        panel: View,
        stats: UnityStatsSnapshot?,
        save: UnitySavePayload?,
        onShowPanel: (Int) -> Unit
    ) {
        // Tab navigation
        panel.findViewById<View?>(R.id.tab_leaderboard)?.setOnClickListener {
            onShowPanel(R.layout.panel_leaderboard)
        }
        
        // Button navigation
        panel.findViewById<View?>(R.id.btn_profile_edit)?.setOnClickListener {
            onShowPanel(R.layout.panel_profile_edit)
        }
        
        panel.findViewById<View?>(R.id.btn_profile_settings)?.setOnClickListener {
            onShowPanel(R.layout.panel_settings)
        }
        
        panel.findViewById<View?>(R.id.btn_profile_friends)?.setOnClickListener {
            onShowPanel(R.layout.panel_friends_leaderboard)
        }
        
        panel.findViewById<View?>(R.id.btn_profile_logout)?.setOnClickListener {
            Toast.makeText(context, context.getString(R.string.toast_logout_placeholder), Toast.LENGTH_SHORT).show()
        }
        
        // Display friend code
        val friendCode = auth.currentUser?.uid ?: "Not logged in"
        panel.findViewById<TextView?>(R.id.text_profile_friend_code)?.text = friendCode
        
        // Display user info
        val user = auth.currentUser
        panel.findViewById<TextView?>(R.id.text_profile_name)?.text = user?.displayName ?: "Player"
        panel.findViewById<TextView?>(R.id.text_profile_email)?.text = user?.email ?: "Not logged in"
        
        // Copy friend code button
        panel.findViewById<Button?>(R.id.btn_copy_friend_code)?.setOnClickListener {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Friend Code", friendCode)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, context.getString(R.string.toast_friend_code_copied), Toast.LENGTH_SHORT).show()
        }
        
        // Set cosmetic tag based on stats
        val tag = panel.findViewById<TextView?>(R.id.text_profile_tag)
        stats?.let { s ->
            when {
                s.totalEnergyGenerated > 1000000.0 -> {
                    tag?.text = "🏆 VETERAN"
                    tag?.setBackgroundColor(0xFFFFD700.toInt())
                }
                s.totalEnergyGenerated > 10000.0 -> {
                    tag?.text = "⚡ ACTIVE"
                    tag?.setBackgroundColor(0xFFFFC107.toInt())
                }
                else -> {
                    tag?.text = "🌟 NEWBIE"
                    tag?.setBackgroundColor(0xFF4CAF50.toInt())
                }
            }
        }
        
        // Bind data
        bindData(panel, stats, save)
    }
    
    /**
     * Binds profile data to UI elements.
     */
    fun bindData(panel: View, stats: UnityStatsSnapshot?, save: UnitySavePayload?) {
        panel.findViewById<TextView?>(R.id.text_profile_energy_total)?.text = stats?.let {
            context.getString(R.string.profile_energy_total, formatUtils.formatEnergy(it.totalEnergyGenerated))
        } ?: context.getString(R.string.profile_energy_total, context.getString(R.string.timestamp_unknown))
        
        panel.findViewById<TextView?>(R.id.text_profile_energy_per_second)?.text = stats?.let {
            context.getString(R.string.profile_energy_per_second, formatUtils.formatEnergy(it.energyPerSecond))
        } ?: context.getString(R.string.profile_energy_per_second, context.getString(R.string.timestamp_unknown))
        
        panel.findViewById<TextView?>(R.id.text_profile_generators)?.text = stats?.let {
            context.getString(R.string.profile_generators_count, it.activeGenerators)
        } ?: context.getString(R.string.profile_generators_count, 0)
        
        panel.findViewById<TextView?>(R.id.text_profile_offline)?.text = stats?.let {
            context.getString(R.string.profile_offline_time, formatUtils.formatDuration(it.offlineSeconds))
        } ?: context.getString(R.string.profile_offline_time, context.getString(R.string.timestamp_unknown))
        
        panel.findViewById<TextView?>(R.id.text_profile_last_sync)?.text = save?.let {
            context.getString(R.string.profile_last_sync, formatUtils.formatTimestamp(it.timestampUtc))
        } ?: context.getString(R.string.profile_last_sync, context.getString(R.string.timestamp_unknown))
    }
}
