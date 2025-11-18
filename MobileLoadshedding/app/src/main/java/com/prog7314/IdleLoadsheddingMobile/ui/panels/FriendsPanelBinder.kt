package com.prog7314.IdleLoadsheddingMobile.ui.panels

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.prog7314.IdleLoadsheddingMobile.R
import com.prog7314.IdleLoadsheddingMobile.data.models.FriendProfile
import com.prog7314.IdleLoadsheddingMobile.data.models.FriendsPayload
import com.prog7314.IdleLoadsheddingMobile.unity.FriendsList
import com.prog7314.IdleLoadsheddingMobile.utils.FormatUtils
import com.prog7314.IdleLoadsheddingMobile.utils.FriendCodeGenerator
import com.prog7314.IdleLoadsheddingMobile.utils.FriendCodeResolver
import kotlinx.coroutines.launch

/**
 * FriendsPanelBinder handles the friends list panel UI.
 * Handles friends panel configuration and data binding.
 * Extracted from MainActivity to separate concerns.
 */
class FriendsPanelBinder(
    private val activity: AppCompatActivity,
    private val formatUtils: FormatUtils
) {
    
    /**
     * Configures friends panel UI elements and event handlers.
     */
    fun configure(
        panel: View,
        friends: FriendsPayload?,
        onShowPanel: (Int) -> Unit
    ) {
        Log.d("FriendsPanelBinder", "Configuring friends leaderboard panel")
        
        // Header tabs: navigate between profile, leaderboard, and friends panels
        panel.findViewById<View?>(R.id.tab_profile)?.setOnClickListener {
            onShowPanel(R.layout.panel_profile)
        }

        panel.findViewById<View?>(R.id.tab_leaderboard)?.setOnClickListener {
            onShowPanel(R.layout.panel_leaderboard)
        }

        // Back to global leaderboard
        panel.findViewById<TextView?>(R.id.btn_show_global)?.setOnClickListener {
            onShowPanel(R.layout.panel_leaderboard)
        }

        // Refresh friends button
        panel.findViewById<TextView?>(R.id.btn_show_friends)?.setOnClickListener {
            Log.d("FriendsPanelBinder", "Refreshing friends list from backend")
            FriendsList.fetchBackendFriends { result ->
                result.onSuccess { payload ->
                    Log.d("FriendsPanelBinder", "SUCCESS: Friends fetched with ${payload.friends.size} entries")
                }.onFailure { error ->
                    Log.e("FriendsPanelBinder", "FAILED: Friends fetch error: ${error.message}", error)
                    Toast.makeText(activity, "Failed to refresh friends list", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Add friend button
        panel.findViewById<Button?>(R.id.btn_add_friend)?.setOnClickListener {
            val codeInput = panel.findViewById<EditText?>(R.id.input_friend_code)
            val statusText = panel.findViewById<TextView?>(R.id.text_add_friend_status)
            val friendCode = codeInput?.text?.toString()?.trim().orEmpty()
            
            // Validate input
            if (friendCode.isEmpty()) {
                Toast.makeText(activity, activity.getString(R.string.toast_friend_code_empty), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            val cleanCode = FriendCodeGenerator.removeFormatting(friendCode)
            if (!FriendCodeGenerator.isValidFriendCodeFormat(cleanCode)) {
                Toast.makeText(activity, "Invalid friend code format", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            // Show loading state
            statusText?.isVisible = true
            statusText?.text = "Looking up friend code..."
            
            // Resolve the friend code to Firebase UID
            activity.lifecycleScope.launch {
                val firebaseUid = FriendCodeResolver.resolveFriendCode(cleanCode)
                
                if (firebaseUid != null) {
                    // Add friend using the resolved Firebase UID
                    FriendsList.updateFriend(
                        firebaseUid,
                        activity.getString(R.string.friend_placeholder_name, cleanCode),
                        15.0,
                        true
                    )
                    Toast.makeText(activity, activity.getString(R.string.toast_friend_invite_sent), Toast.LENGTH_SHORT).show()
                    codeInput?.text?.clear()
                    statusText?.isVisible = false
                    
                    // Refresh friends list after adding
                    FriendsList.fetchBackendFriends { result ->
                        result.onFailure { error ->
                            Log.e("FriendsPanelBinder", "Failed to refresh after adding friend: ${error.message}", error)
                        }
                    }
                } else {
                    statusText?.text = "Friend code not found. Please check and try again."
                    statusText?.setTextColor(0xFFE53935.toInt())
                }
            }
        }

        // Bind data
        bindData(panel, friends)
        
        // Fetch if no cached data
        if (friends == null) {
            Log.d("FriendsPanelBinder", "No cached friends data, fetching from backend API")
            FriendsList.fetchBackendFriends { result ->
                result.onSuccess { payload ->
                    Log.d("FriendsPanelBinder", "Initial friends fetch successful: ${payload.friends.size} friends")
                }.onFailure { error ->
                    Log.e("FriendsPanelBinder", "Failed to fetch initial friends: ${error.message}", error)
                    FriendsList.requestUnityFriends()
                }
            }
        } else {
            Log.d("FriendsPanelBinder", "Using cached friends data with ${friends.friends.size} friends")
        }
    }
    
    /**
     * Binds friends data to UI elements.
     */
    fun bindData(panel: View, payload: FriendsPayload?) {
        val loading = panel.findViewById<ProgressBar?>(R.id.friends_loading)
        val empty = panel.findViewById<View?>(R.id.text_friends_empty)
        val listContainer = panel.findViewById<LinearLayout?>(R.id.friends_list_container)
        val lastUpdated = panel.findViewById<TextView?>(R.id.text_friends_last_updated)
        val topThreeContainer = panel.findViewById<View?>(R.id.top_three_container)

        if (payload == null) {
            // Show loading state
            loading?.isVisible = true
            empty?.isVisible = false
            listContainer?.isVisible = false
            topThreeContainer?.isVisible = false
            lastUpdated?.text = "Loading friends..."
            return
        }

        loading?.isVisible = false
        lastUpdated?.text = activity.getString(R.string.friends_last_updated, formatUtils.formatTimestamp(payload.timestampUtc))

        val entries = payload.friends
        if (entries.isEmpty()) {
            // Show empty state
            empty?.isVisible = true
            listContainer?.isVisible = false
            topThreeContainer?.isVisible = false
            Log.d("FriendsPanelBinder", "Friends list is empty - showing empty state")
        } else {
            empty?.isVisible = false
            topThreeContainer?.isVisible = true

            // Populate top 3 slots
            val top1 = entries.getOrNull(0)
            val top2 = entries.getOrNull(1)
            val top3 = entries.getOrNull(2)

            panel.findViewById<TextView?>(R.id.text_top1_name)?.text = top1?.displayName ?: "--"
            panel.findViewById<TextView?>(R.id.text_top1_score)?.text = top1?.let { formatUtils.formatEnergy(it.contributionPerHour) } ?: "--"

            panel.findViewById<TextView?>(R.id.text_top2_name)?.text = top2?.displayName ?: "--"
            panel.findViewById<TextView?>(R.id.text_top2_score)?.text = top2?.let { formatUtils.formatEnergy(it.contributionPerHour) } ?: "--"

            panel.findViewById<TextView?>(R.id.text_top3_name)?.text = top3?.displayName ?: "--"
            panel.findViewById<TextView?>(R.id.text_top3_score)?.text = top3?.let { formatUtils.formatEnergy(it.contributionPerHour) } ?: "--"

            // Hide second/third slots if not enough entries
            panel.findViewById<View?>(R.id.top_two_container)?.isVisible = top2 != null
            panel.findViewById<View?>(R.id.top_three_container_slot)?.isVisible = top3 != null

            // Render remaining entries (4th and beyond)
            val remaining = if (entries.size > 3) entries.drop(3) else emptyList()
            if (remaining.isEmpty()) {
                listContainer?.isVisible = false
            } else {
                listContainer?.isVisible = true
                listContainer?.let { renderEntries(it, remaining, startRank = 4) }
            }

            Log.d("FriendsPanelBinder", "Displaying ${entries.size} friends (top 3 + list)")
        }
    }
    
    /**
     * Renders friends entries into the container.
     */
    private fun renderEntries(
        container: LinearLayout,
        entries: List<FriendProfile>,
        startRank: Int = 1
    ) {
        container.removeAllViews()
        entries.forEachIndexed { index, friend ->
            val row = LayoutInflater.from(activity).inflate(R.layout.view_leaderboard_row, container, false)
            row.findViewById<TextView?>(R.id.text_rank)?.text = "#${startRank + index}"
            
            // Add cosmetic tags based on contribution
            val displayNameWithTag = buildString {
                append(friend.displayName)
                append(" ")
                when {
                    friend.contributionPerHour > 1000.0 -> append("🏆")
                    friend.contributionPerHour > 100.0 -> append("⚡")
                    else -> append("🌟")
                }
            }
            
            row.findViewById<TextView?>(R.id.text_entry_title)?.text = displayNameWithTag
            row.findViewById<TextView?>(R.id.text_entry_detail)?.text =
                "${formatUtils.formatEnergy(friend.contributionPerHour)} /hr • ${formatUtils.formatTimestamp(friend.lastInteractionUtc)}"
            
            // Online presence icons
            val icon = row.findViewById<ImageView?>(R.id.image_entry_status)
            icon?.setImageResource(
                if (friend.isOnline) android.R.drawable.presence_online else android.R.drawable.presence_invisible
            )
            container.addView(row)
        }
    }
}
