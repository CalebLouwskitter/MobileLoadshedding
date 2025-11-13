package com.prog7314.IdleLoadsheddingMobile.ui.panels

import android.app.Activity
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.view.isVisible
import com.prog7314.IdleLoadsheddingMobile.R
import com.prog7314.IdleLoadsheddingMobile.data.models.UnityLeaderboardPayload
import com.prog7314.IdleLoadsheddingMobile.unity.Leaderboard
import com.prog7314.IdleLoadsheddingMobile.utils.FormatUtils

/**
 * Handles leaderboard panel configuration and data binding.
 * Extracted from MainActivity to separate concerns.
 */
class LeaderboardPanelBinder(
    private val activity: Activity,
    private val formatUtils: FormatUtils
) {
    
    /**
     * Configures leaderboard panel UI elements and event handlers.
     */
    fun configure(
        panel: View,
        leaderboard: UnityLeaderboardPayload?,
        onShowPanel: (Int) -> Unit
    ) {
        Log.d("LeaderboardPanelBinder", "Configuring leaderboard panel")
        
        // Tab navigation to friends
        panel.findViewById<View?>(R.id.btn_tab_friends)?.setOnClickListener {
            onShowPanel(R.layout.panel_friends_leaderboard)
        }

        // Refresh button
        panel.findViewById<View?>(R.id.btn_tab_global)?.setOnClickListener {
            Log.d("LeaderboardPanelBinder", "Global tab clicked - fetching from API")
            Leaderboard.fetchBackendLeaderboard { result ->
                result.onFailure { error ->
                    Log.e("LeaderboardPanelBinder", "Failed to refresh leaderboard: ${error.message}", error)
                }
            }
        }

        // Bind data
        bindData(panel, leaderboard)
        
        // Fetch if no cached data
        if (leaderboard == null) {
            Log.d("LeaderboardPanelBinder", "No cached leaderboard data, fetching from API")
            Leaderboard.fetchBackendLeaderboard { result ->
                result.onFailure { error ->
                    Log.e("LeaderboardPanelBinder", "Failed to fetch initial leaderboard: ${error.message}", error)
                }
            }
        } else {
            Log.d("LeaderboardPanelBinder", "Using cached leaderboard data with ${leaderboard.entries.size} entries")
        }
    }
    
    /**
     * Binds leaderboard data to UI elements.
     */
    fun bindData(panel: View, payload: UnityLeaderboardPayload?) {
        val loading = panel.findViewById<ProgressBar?>(R.id.leaderboard_loading)
        val empty = panel.findViewById<TextView?>(R.id.text_leaderboard_empty)
        val listContainer = panel.findViewById<LinearLayout?>(R.id.leaderboard_list_container)
        val lastUpdated = panel.findViewById<TextView?>(R.id.text_leaderboard_last_updated)

        if (payload == null) {
            // Show loading state
            loading?.isVisible = true
            empty?.isVisible = false
            listContainer?.isVisible = false
            return
        }

        loading?.isVisible = false
        lastUpdated?.text = activity.getString(R.string.leaderboard_last_updated, formatUtils.formatTimestamp(payload.timestampUtc))

        val entries = payload.entries
        if (entries.isEmpty()) {
            // Show empty state
            empty?.isVisible = true
            listContainer?.isVisible = false
        } else {
            empty?.isVisible = false
            listContainer?.isVisible = true
            listContainer?.let { renderEntries(it, entries) }
        }
    }
    
    /**
     * Renders leaderboard entries into the container.
     */
    private fun renderEntries(
        container: LinearLayout,
        entries: List<UnityLeaderboardPayload.Entry>
    ) {
        container.removeAllViews()
        entries.forEachIndexed { index, entry ->
            val row = LayoutInflater.from(activity).inflate(R.layout.view_leaderboard_row, container, false)
            row.findViewById<TextView?>(R.id.text_rank)?.text = "#${index + 1}"
            
            // Add cosmetic tags based on score
            val displayNameWithTag = buildString {
                append(entry.displayName)
                append(" ")
                when {
                    entry.score > 1000000.0 -> append("🏆")
                    entry.score > 100000.0 -> append("⚡")
                    else -> append("🌟")
                }
            }
            
            row.findViewById<TextView?>(R.id.text_entry_title)?.text = displayNameWithTag
            row.findViewById<TextView?>(R.id.text_entry_detail)?.text = formatUtils.formatEnergy(entry.score)
            
            // Rank icons
            val icon = row.findViewById<ImageView?>(R.id.image_entry_status)
            val iconRes = when (index) {
                0 -> android.R.drawable.star_big_on
                in 1..2 -> android.R.drawable.btn_star_big_on
                else -> android.R.drawable.btn_star_big_off
            }
            icon?.setImageResource(iconRes)
            container.addView(row)
        }
    }
}
