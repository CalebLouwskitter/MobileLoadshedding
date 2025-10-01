package com.prog7314.IdleLoadsheddingMobile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.Toast
import com.prog7314.IdleLoadsheddingMobile.unity.UnityBridge
import com.unity3d.player.UnityPlayerGameActivity

class MainActivity : UnityPlayerGameActivity(), UnityBridge.Listener {

    private lateinit var modalContainer: FrameLayout
    private lateinit var modalScrim: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val overlay = LayoutInflater.from(this).inflate(R.layout.activity_main, null)
        modalContainer = overlay.findViewById(R.id.modal_container)
        modalScrim = overlay.findViewById(R.id.modal_scrim)

        val layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )

        addContentView(overlay, layoutParams)

        // Register for callbacks from Unity
        UnityBridge.registerListener(this)

        modalScrim.setOnClickListener { hidePanel() }

        overlay.findViewById<ImageButton>(R.id.btn_hud_upgrades).setOnClickListener {
            showPanel(R.layout.panel_electricity_upgrade)
        }

        overlay.findViewById<ImageButton>(R.id.btn_hud_profile).setOnClickListener {
            showPanel(R.layout.panel_profile)
        }

        overlay.findViewById<Button>(R.id.btn_nav_level_up).setOnClickListener {
            showPanel(R.layout.panel_building_upgrade)
        }

        overlay.findViewById<Button>(R.id.btn_nav_power).setOnClickListener {
            showPanel(R.layout.panel_electricity_upgrade)
        }

        overlay.findViewById<Button>(R.id.btn_nav_tasks).setOnClickListener {
            showPanel(R.layout.panel_tasks_board)
        }
    }

    override fun onDestroy() {
        if (this::modalContainer.isInitialized) {
            // no-op; just ensuring proper lifecycle
        }
        UnityBridge.unregisterListener(this)
        super.onDestroy()
    }

    // Unity -> Android UI callbacks
    override fun onScoreUpdated(score: Int) {
        // Update your UI here (e.g., a TextView) or keep it simple with a toast for now
        Toast.makeText(this, "Score: $score", Toast.LENGTH_SHORT).show()
    }

    override fun onGameEvent(event: String) {
        Toast.makeText(this, "Game event: $event", Toast.LENGTH_SHORT).show()
    }

    private fun showPanel(layoutResId: Int) {
        val panelView = LayoutInflater.from(this).inflate(layoutResId, modalContainer, false)
        modalContainer.removeAllViews()
        modalContainer.addView(panelView)
        modalContainer.visibility = View.VISIBLE
        modalScrim.visibility = View.VISIBLE

        panelView.findViewById<View?>(R.id.close_button)?.setOnClickListener { hidePanel() }

        when (layoutResId) {
            R.layout.panel_electricity_upgrade -> configureElectricityPanel(panelView)
            R.layout.panel_building_upgrade -> configureBuildingPanel(panelView)
            R.layout.panel_profile -> configureProfilePanel(panelView)
            R.layout.panel_profile_edit -> configureProfileEditPanel(panelView)
            R.layout.panel_leaderboard -> configureLeaderboardPanel(panelView)
            R.layout.panel_friends_leaderboard -> configureFriendsLeaderboardPanel(panelView)
        }
    }

    private fun hidePanel() {
        modalContainer.removeAllViews()
        modalContainer.visibility = View.GONE
        modalScrim.visibility = View.GONE
    }

    private fun configureElectricityPanel(panel: View) {
        panel.findViewById<View?>(R.id.tab_building)?.setOnClickListener {
            showPanel(R.layout.panel_building_upgrade)
        }
    }

    private fun configureBuildingPanel(panel: View) {
        panel.findViewById<View?>(R.id.tab_electricity)?.setOnClickListener {
            showPanel(R.layout.panel_electricity_upgrade)
        }
    }

    private fun configureProfilePanel(panel: View) {
        panel.findViewById<View?>(R.id.tab_leaderboard)?.setOnClickListener {
            showPanel(R.layout.panel_leaderboard)
        }

        panel.findViewById<View?>(R.id.btn_profile_settings)?.setOnClickListener {
            showPanel(R.layout.panel_settings)
        }

        panel.findViewById<View?>(R.id.btn_profile_logout)?.setOnClickListener {
            UnityBridge.showToast(this, "Logging out (placeholder)")
            hidePanel()
        }

        panel.findViewById<View?>(R.id.btn_profile_details)?.setOnClickListener {
            UnityBridge.showToast(this, "Profile details placeholder")
        }
    }

    private fun configureProfileEditPanel(panel: View) {
        panel.findViewById<Button?>(R.id.btn_cancel_profile)?.setOnClickListener {
            showPanel(R.layout.panel_profile)
        }

        panel.findViewById<Button?>(R.id.btn_save_profile)?.setOnClickListener {
            UnityBridge.showToast(this, "Profile saved (placeholder)")
            showPanel(R.layout.panel_profile)
        }
    }

    private fun configureLeaderboardPanel(panel: View) {
        panel.findViewById<Button?>(R.id.btn_tab_friends)?.setOnClickListener {
            showPanel(R.layout.panel_friends_leaderboard)
        }

        panel.findViewById<Button?>(R.id.btn_tab_global)?.setOnClickListener {
            UnityBridge.showToast(this, "Showing global leaderboard")
        }
    }

    private fun configureFriendsLeaderboardPanel(panel: View) {
        panel.findViewById<Button?>(R.id.btn_show_global)?.setOnClickListener {
            showPanel(R.layout.panel_leaderboard)
        }

        panel.findViewById<Button?>(R.id.btn_show_friends)?.setOnClickListener {
            UnityBridge.showToast(this, "Showing friends leaderboard")
        }

        panel.findViewById<Button?>(R.id.btn_add_friend)?.setOnClickListener {
            UnityBridge.showToast(this, "Friend invite sent (placeholder)")
        }
    }
}