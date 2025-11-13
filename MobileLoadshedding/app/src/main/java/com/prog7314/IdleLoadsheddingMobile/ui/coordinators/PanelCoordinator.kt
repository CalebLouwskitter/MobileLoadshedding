package com.prog7314.IdleLoadsheddingMobile.ui.coordinators

import android.util.Log
import android.view.View
import com.prog7314.IdleLoadsheddingMobile.R
import com.prog7314.IdleLoadsheddingMobile.ui.panels.*

/**
 * Coordinates panel configuration by routing to appropriate binders.
 * 
 * This class eliminates the 8 separate configure* methods in MainActivity
 * by providing a single entry point that dispatches to the correct binder.
 * 
 * WHY THIS EXISTS:
 * - Reduces MainActivity's panel configuration code from 8 methods to 1
 * - Centralizes panel-to-binder mapping logic
 * - Makes it easy to add new panels without touching MainActivity
 * - Provides consistent panel lifecycle (configure -> bind -> update)
 */
class PanelCoordinator(
    private val stateManager: StateManager,
    private val buildingPanelBinder: BuildingPanelBinder,
    private val districtsPanelBinder: DistrictsPanelBinder,
    private val electricityPanelBinder: ElectricityPanelBinder,
    private val profilePanelBinder: ProfilePanelBinder,
    private val settingsPanelBinder: SettingsPanelBinder,
    private val leaderboardPanelBinder: LeaderboardPanelBinder,
    private val friendsPanelBinder: FriendsPanelBinder,
    private val tasksPanelBinder: TasksPanelBinder,
    private val profileEditPanelBinder: ProfileEditPanelBinder
) {
    
    /**
     * Configure a panel by dispatching to the appropriate binder.
     * This is called once when a panel is first shown.
     * 
     * @param layoutResId The layout resource ID of the panel
     * @param panelView The inflated panel view
     * @param onNavigate Callback for navigating to other panels
     */
    fun configurePanel(layoutResId: Int, panelView: View, onNavigate: (Int) -> Unit) {
        when (layoutResId) {
            R.layout.panel_electricity_upgrade -> {
                electricityPanelBinder.configure(
                    panelView,
                    stateManager.latestStats,
                    stateManager.latestSave,
                    stateManager.currentActiveEvent,
                    stateManager.eventHistory,
                    onNavigate
                )
            }
            
            R.layout.panel_building_upgrade -> {
                buildingPanelBinder.configure(
                    panelView,
                    stateManager.latestStats,
                    onNavigate
                )
            }
            
            R.layout.panel_profile -> {
                profilePanelBinder.configure(
                    panelView,
                    stateManager.latestStats,
                    stateManager.latestSave,
                    onNavigate
                )
            }
            
            R.layout.panel_profile_edit -> {
                profileEditPanelBinder.configure(panelView, onNavigate)
            }
            
            R.layout.panel_leaderboard -> {
                leaderboardPanelBinder.configure(
                    panelView,
                    stateManager.latestLeaderboard,
                    onNavigate
                )
            }
            
            R.layout.panel_settings -> {
                settingsPanelBinder.configure(panelView, stateManager.latestSave)
            }
            
            R.layout.panel_friends_leaderboard -> {
                friendsPanelBinder.configure(
                    panelView,
                    stateManager.latestFriends,
                    onNavigate
                )
            }
            
            R.layout.panel_tasks_board -> {
                tasksPanelBinder.configure(
                    panelView,
                    stateManager.eventHistory,
                    stateManager.currentActiveEvent
                )
            }
        }
    }
    
    /**
     * Update currently visible panel with fresh data.
     * This is called when state changes while a panel is open.
     * 
     * @param layoutResId The layout resource ID of the currently visible panel
     * @param panelView The panel view to update
     */
    fun updateVisiblePanel(layoutResId: Int, panelView: View) {
        when (layoutResId) {
            R.layout.panel_electricity_upgrade -> {
                electricityPanelBinder.bindData(
                    panelView,
                    stateManager.latestStats,
                    stateManager.latestSave,
                    stateManager.currentActiveEvent,
                    stateManager.eventHistory
                )
            }
            
            R.layout.panel_building_upgrade -> {
                buildingPanelBinder.bindData(panelView, stateManager.latestStats)
            }
            
            R.layout.panel_profile -> {
                profilePanelBinder.bindData(
                    panelView,
                    stateManager.latestStats,
                    stateManager.latestSave
                )
            }
            
			R.layout.panel_leaderboard -> {
				val payload = stateManager.latestLeaderboard
				Log.d("PanelCoordinator", "📊 Updating leaderboard panel (payload: ${payload != null}, entries: ${payload?.entries?.size ?: 0})")
				leaderboardPanelBinder.bindData(panelView, payload)
			}            R.layout.panel_friends_leaderboard -> {
                friendsPanelBinder.bindData(panelView, stateManager.latestFriends)
            }
            
            R.layout.panel_settings -> {
                settingsPanelBinder.configure(panelView, stateManager.latestSave)
            }
            
            R.layout.panel_tasks_board -> {
                tasksPanelBinder.configure(
                    panelView,
                    stateManager.eventHistory,
                    stateManager.currentActiveEvent
                )
            }
        }
    }
}
