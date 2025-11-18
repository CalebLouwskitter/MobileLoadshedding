package com.prog7314.IdleLoadsheddingMobile.ui.coordinators

import android.content.Intent
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.prog7314.IdleLoadsheddingMobile.BuildConfig
import com.prog7314.IdleLoadsheddingMobile.R
import com.prog7314.IdleLoadsheddingMobile.networking.FirebasePush.FcmIntegrationHelper
import com.prog7314.IdleLoadsheddingMobile.networking.FirebasePush.FcmTokenManager
import com.prog7314.IdleLoadsheddingMobile.networking.FirebasePush.FirebaseCloudMessenger
import com.prog7314.IdleLoadsheddingMobile.networking.FirebasePush.NotificationSimulator
import com.prog7314.IdleLoadsheddingMobile.data.models.NotificationType
import com.prog7314.IdleLoadsheddingMobile.ui.panels.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
                settingsPanelBinder.configure(panelView, stateManager.latestSave, onNavigate)
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
            
            R.layout.panel_debug_notifications -> {
                configureDebugNotificationsPanel(panelView)
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
            
            R.layout.panel_debug_notifications -> {
                // Debug panel doesn't need updates
            }
        }
    }
    
    /**
     * Configure the debug notifications panel.
     * This panel allows testing push notifications without needing Firebase Console.
     */
    private fun configureDebugNotificationsPanel(panelView: View) {
        val context = panelView.context
        val activity = context as? AppCompatActivity
        val lifecycleOwner = activity
        val permissionStatus = panelView.findViewById<TextView?>(R.id.text_status_permission)
        val tokenStatus = panelView.findViewById<TextView?>(R.id.text_status_token)
        val backendStatus = panelView.findViewById<TextView?>(R.id.text_status_backend)
        val resultStatus = panelView.findViewById<TextView?>(R.id.text_last_notification_result)
        val cloudStatus = panelView.findViewById<TextView?>(R.id.text_cloud_result)

        fun TextView?.setStatus(ok: Boolean, message: String) {
            this?.text = if (ok) "✅ $message" else "⚠️ $message"
            val colorRes = if (ok) R.color.modal_button_primary else R.color.modal_button_danger
            this?.setTextColor(ContextCompat.getColor(context, colorRes))
        }

        fun logResult(success: Boolean, label: String) {
            val formatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            val stamp = formatter.format(Date())
            resultStatus?.text = if (success) {
                "✅ $label • $stamp"
            } else {
                "⚠️ $label • $stamp"
            }
            val colorRes = if (success) R.color.modal_button_primary else R.color.modal_button_danger
            resultStatus?.setTextColor(ContextCompat.getColor(context, colorRes))
        }

        fun refreshStatuses() {
            val permissionGranted = activity?.let { FcmIntegrationHelper.hasNotificationPermission(it) } ?: false
            permissionStatus.setStatus(permissionGranted, if (permissionGranted) {
                "Notifications allowed"
            } else {
                "Permission required"
            })

            lifecycleOwner?.lifecycleScope?.launch {
                val manager = FcmTokenManager.getInstance(context)
                val token = manager.fcmToken.first()
                val registered = manager.lastRegisteredToken.first()
                val hasToken = !token.isNullOrBlank()
                tokenStatus.setStatus(hasToken, if (hasToken) {
                    "Token cached • …${token!!.takeLast(6)}"
                } else {
                    "Waiting for token"
                })
                val backendDisabled = !BuildConfig.ENABLE_BACKEND_TOKEN_REGISTRATION
                val backendSynced = hasToken && token == registered
                when {
                    backendDisabled -> backendStatus.setStatus(true, "Backend sync bypassed (Firebase only)")
                    backendSynced -> backendStatus.setStatus(true, "Synced with backend")
                    else -> backendStatus.setStatus(false, "Tap Sync Token")
                }
            }
        }
        
        panelView.findViewById<View>(com.prog7314.IdleLoadsheddingMobile.R.id.btn_test_event_started)?.setOnClickListener {
            logResult(false, "⏳ Sending to Firebase...")
            FirebaseCloudMessenger.sendCloudNotification(NotificationType.EVENT_STARTED) { success, message ->
                logResult(success, message)
            }
        }
        
        panelView.findViewById<View>(com.prog7314.IdleLoadsheddingMobile.R.id.btn_test_event_ending_soon)?.setOnClickListener {
            logResult(false, "⏳ Sending to Firebase...")
            FirebaseCloudMessenger.sendCloudNotification(NotificationType.EVENT_ENDING_SOON) { success, message ->
                logResult(success, message)
            }
        }
        
        panelView.findViewById<View>(com.prog7314.IdleLoadsheddingMobile.R.id.btn_test_friend_request)?.setOnClickListener {
            logResult(false, "⏳ Sending to Firebase...")
            FirebaseCloudMessenger.sendCloudNotification(NotificationType.FRIEND_REQUEST) { success, message ->
                logResult(success, message)
            }
        }
        
        panelView.findViewById<View>(com.prog7314.IdleLoadsheddingMobile.R.id.btn_test_friend_accepted)?.setOnClickListener {
            logResult(false, "⏳ Sending to Firebase...")
            FirebaseCloudMessenger.sendCloudNotification(NotificationType.FRIEND_ACCEPTED) { success, message ->
                logResult(success, message)
            }
        }
        
        panelView.findViewById<View>(com.prog7314.IdleLoadsheddingMobile.R.id.btn_test_system_announcement)?.setOnClickListener {
            logResult(false, "⏳ Sending to Firebase...")
            FirebaseCloudMessenger.sendCloudNotification(NotificationType.SYSTEM_ANNOUNCEMENT) { success, message ->
                logResult(success, message)
            }
        }
        
        panelView.findViewById<View>(com.prog7314.IdleLoadsheddingMobile.R.id.btn_test_maintenance)?.setOnClickListener {
            logResult(false, "⏳ Sending to Firebase...")
            FirebaseCloudMessenger.sendCloudNotification(NotificationType.MAINTENANCE_REQUIRED) { success, message ->
                logResult(success, message)
            }
        }
        
        panelView.findViewById<View>(com.prog7314.IdleLoadsheddingMobile.R.id.btn_test_general)?.setOnClickListener {
            logResult(false, "⏳ Sending to Firebase...")
            FirebaseCloudMessenger.sendCloudNotification(NotificationType.GENERAL) { success, message ->
                logResult(success, message)
            }
        }

        panelView.findViewById<View>(R.id.btn_request_notification_permission)?.setOnClickListener {
            activity?.let { act ->
                FcmIntegrationHelper.initialize(act) {
                    refreshStatuses()
                }
            }
        }

        panelView.findViewById<View>(R.id.btn_force_token_refresh)?.setOnClickListener {
            activity?.let { act ->
                FcmIntegrationHelper.forceTokenRefresh(act) { success ->
                    act.runOnUiThread {
                        logResult(success, if (success) "Token synced" else "Token sync failed")
                        refreshStatuses()
                    }
                }
            }
        }

        panelView.findViewById<View>(R.id.btn_open_notification_settings)?.setOnClickListener {
            activity?.let { act ->
                try {
                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, act.packageName)
                    }
                    act.startActivity(intent)
                } catch (e: Exception) {
                    Log.w("PanelCoordinator", "Unable to open notification settings", e)
                }
            }
        }

        panelView.findViewById<View>(R.id.btn_send_cloud_notification)?.setOnClickListener {
            cloudStatus?.text = "⏳ Sending request to Firebase..."
            cloudStatus?.setTextColor(ContextCompat.getColor(context, R.color.modal_text_on_light))
            FirebaseCloudMessenger.sendDemoNotification(NotificationType.EVENT_STARTED) { success, message ->
                logResult(success, "Firebase cloud: $message")
                val prefix = if (success) "✅" else "⚠️"
                val colorRes = if (success) R.color.modal_button_primary else R.color.modal_button_danger
                cloudStatus?.text = "$prefix $message"
                cloudStatus?.setTextColor(ContextCompat.getColor(context, colorRes))
            }
        }

        refreshStatuses()
    }
}
