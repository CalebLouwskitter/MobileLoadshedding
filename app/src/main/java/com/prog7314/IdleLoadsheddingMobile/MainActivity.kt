package com.prog7314.IdleLoadsheddingMobile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
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

        addContentView(
            overlay,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        UnityBridge.registerListener(this)
        modalScrim.setOnClickListener { hidePanel() }

        // HUD Buttons
        overlay.findViewById<ImageButton>(R.id.btn_hud_upgrades).setOnClickListener {
            showPanel(R.layout.panel_electricity_upgrade)
        }
        overlay.findViewById<ImageButton>(R.id.btn_hud_profile).setOnClickListener {
            showPanel(R.layout.panel_profile)
        }

        // Bottom navigation buttons
        overlay.findViewById<View>(R.id.btn_nav_level_up).setOnClickListener {
            showPanel(R.layout.panel_building_upgrade)
        }
        overlay.findViewById<View>(R.id.btn_nav_power).setOnClickListener {
            showPanel(R.layout.panel_electricity_upgrade)
        }
        overlay.findViewById<View>(R.id.btn_nav_tasks).setOnClickListener {
            showPanel(R.layout.panel_tasks_board)
        }
    }

    override fun onDestroy() {
        UnityBridge.unregisterListener(this)
        super.onDestroy()
    }

    override fun onScoreUpdated(score: Int) {
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
        panel.findViewById<View?>(R.id.btn_cancel_profile)?.setOnClickListener {
            showPanel(R.layout.panel_profile)
        }

        panel.findViewById<View?>(R.id.btn_save_profile)?.setOnClickListener {
            UnityBridge.showToast(this, "Profile saved (placeholder)")
            showPanel(R.layout.panel_profile)
        }
    }

    private fun configureLeaderboardPanel(panel: View) {
        panel.findViewById<TextView?>(R.id.btn_tab_friends)?.setOnClickListener {
            showPanel(R.layout.panel_friends_leaderboard)
        }
        panel.findViewById<TextView?>(R.id.btn_tab_global)?.setOnClickListener {
            UnityBridge.showToast(this, "Showing global leaderboard")
        }

        panel.findViewById<TextView?>(R.id.tab_profile)?.setOnClickListener {
            showPanel(R.layout.panel_profile)
        }

        panel.findViewById<TextView?>(R.id.tab_leaderboard)?.setOnClickListener {
            UnityBridge.showToast(this, "Already on Leaderboard")
        }
    }

    private fun configureFriendsLeaderboardPanel(panel: View) {
        // Friends/Global toggle
        panel.findViewById<TextView?>(R.id.btn_tab_friends)?.setOnClickListener {
            UnityBridge.showToast(this, "Already on Friends")
        }
        panel.findViewById<TextView?>(R.id.btn_tab_global)?.setOnClickListener {
            showPanel(R.layout.panel_leaderboard)
        }

    }
}
