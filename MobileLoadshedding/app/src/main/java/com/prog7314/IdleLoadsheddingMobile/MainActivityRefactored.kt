package com.prog7314.IdleLoadsheddingMobile

/**
 * REFACTORED MAINACTIVITY - Drastically reduced bloat version
 * 
 * This is a reference implementation showing how MainActivity can be reduced
 * from 620 lines to ~180 lines by using the new coordinator pattern.
 * 
 * Key Changes:
 * 1. ObserverCoordinator replaces 5 separate observer objects
 * 2. StateManager replaces 7+ scattered state variables
 * 3. PanelCoordinator replaces 8 configure* methods
 * 4. UIComponentFactory replaces manual binder instantiation
 * 5. HudManager now handles all HUD updates internally
 * 
 * To apply this refactoring:
 * 1. Review this file to understand the new structure
 * 2. Test that all coordinators work correctly
 * 3. Replace old MainActivity.kt with this version
 * 4. Delete legacy methods (updateHud, renderDistrictsPanel, etc.)
 */

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.prog7314.IdleLoadsheddingMobile.core.UnityBridge
import com.prog7314.IdleLoadsheddingMobile.data.models.GameSyncRequest
import com.prog7314.IdleLoadsheddingMobile.networking.RetrofitClient
import com.unity3d.player.UnityPlayerGameActivity
import com.prog7314.IdleLoadsheddingMobile.ui.coordinators.*
import com.prog7314.IdleLoadsheddingMobile.unity.PowerManager
import com.prog7314.IdleLoadsheddingMobile.unity.SaveSync
import com.prog7314.IdleLoadsheddingMobile.unity.Stats
import kotlinx.coroutines.launch

/**
 * MainActivity - Game Activity Coordinator
 * 
 * This activity's sole responsibility is to:
 * 1. Initialize Unity and UI overlays
 * 2. Wire up HUD button click handlers
 * 3. Delegate all data/UI updates to specialized coordinators
 * 
 * All business logic has been extracted to:
 * - ObserverCoordinator: Unity data callbacks
 * - StateManager: Game state caching
 * - PanelCoordinator: Panel configuration/updates
 * - HudManager: HUD display updates
 * - PanelManager: Panel show/hide lifecycle
 */
class MainActivityRefactored : UnityPlayerGameActivity() {
    
    // All UI components managed by factory
    private lateinit var components: UIComponentFactory.UIComponents
    
    // View references needed for click handlers
    private lateinit var modalContainer: FrameLayout
    private lateinit var modalScrim: View
    
    private val auth = FirebaseAuth.getInstance()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("MainActivity", "onCreate() started")
        
        // 1. Inflate UI overlay
        val overlay = LayoutInflater.from(this).inflate(R.layout.activity_main, null)
        modalContainer = overlay.findViewById(R.id.modal_container)
        modalScrim = overlay.findViewById(R.id.modal_scrim)
        
        // 2. Collect HUD TextView references
        val hudTextViews = UIComponentFactory.HudTextViews(
            textEnergyAmount = overlay.findViewById(R.id.text_currency_amount),
            textEnergyPerSecond = overlay.findViewById(R.id.text_energy_per_second),
            textActiveGenerators = overlay.findViewById(R.id.text_active_generators),
            textEventStatus = overlay.findViewById<TextView>(R.id.text_event_status).apply {
                isVisible = false
            },
            textSafeIdleHours = overlay.findViewById(R.id.text_safe_idle_hours)
        )
        
        // 3. Initialize all UI components via factory
        val factory = UIComponentFactory(this, auth)
        components = factory.createComponents(
            hudTextViews = hudTextViews,
            modalContainer = modalContainer,
            modalScrim = modalScrim,
            onStateChanged = ::onGameStateChanged
        )
        
        // 4. Apply saved locale
        components.localeManager.applySavedLocale()
        
        // 5. Wire up HUD button handlers
        setupHudButtons(overlay)
        
        // 6. Attach overlay to Unity surface
        val layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
        addContentView(overlay, layoutParams)
        
        // 7. Initialize Unity integration
        SaveSync.initialize(this)
        
        window.decorView.post {
            // Hide Unity's internal HUD (we use native Android overlay)
            UnityBridge.hideUnityHud()
            
            // Subscribe to Unity callbacks
            components.observerCoordinator.subscribeAll()
            
            // Request initial data from Unity
            components.observerCoordinator.requestInitialData()
        }
        
        Log.d("MainActivity", "onCreate() completed")
    }
    
    override fun onDestroy() {
        // Clean up all Unity observers to prevent memory leaks
        components.observerCoordinator.unsubscribeAll()
        super.onDestroy()
    }
    
    override fun onResume() {
        super.onResume()
        // Sync game state with server when app returns from background
        syncGameStateWithServer()
    }
    
    /**
     * Synchronize game state with server to calculate offline earnings
     * and validate server-side state.
     */
    private fun syncGameStateWithServer() {
        val stats = components.stateManager.latestStats ?: return
        
        lifecycleScope.launch {
            try {
                val request = GameSyncRequest(
                    moneyEarned = stats.currentMoney,
                    electricityGenerated = stats.energyPerSecond,
                    prestigeLevel = 0 // TODO: Get prestige level from Unity
                )
                
                val response = RetrofitClient.instance.syncGameState(request)
                if (response.isSuccessful) {
                    val syncData = response.body()
                    if (syncData != null) {
                        Log.d("MainActivity", "Game sync successful: offline earnings = ${syncData.offlineEarnings}")
                        // TODO: Update Unity with offline earnings
                        // UnityBridge.applyOfflineEarnings(syncData.offlineEarnings)
                    }
                } else {
                    Log.w("MainActivity", "Game sync failed: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error syncing game state", e)
            }
        }
    }
    
    /**
     * Set up click handlers for all HUD buttons.
     * This is the only place MainActivity handles UI events directly.
     */
    private fun setupHudButtons(overlay: View) {
        // Modal scrim closes any open panel
        modalScrim.setOnClickListener {
            components.panelManager.hidePanel()
        }
        
        // Upgrades button opens electricity panel
        overlay.findViewById<ImageButton>(R.id.btn_hud_upgrades).setOnClickListener {
            showPanel(R.layout.panel_electricity_upgrade)
        }
        
        // Profile button opens profile panel
        overlay.findViewById<ImageButton>(R.id.btn_hud_profile).setOnClickListener {
            showPanel(R.layout.panel_profile)
        }
        
        // Level up button opens building upgrades
        overlay.findViewById<Button>(R.id.btn_nav_level_up).setOnClickListener {
            showPanel(R.layout.panel_building_upgrade)
        }
        
        // Power button (tap) - click generator for burst energy
        overlay.findViewById<Button>(R.id.btn_nav_power).setOnClickListener {
            PowerManager.clickGenerator("generator_1")
            Stats.requestUnitySnapshot()
        }
        
        // Power button (hold) - open electricity panel for tuning
        overlay.findViewById<Button>(R.id.btn_nav_power).setOnLongClickListener {
            showPanel(R.layout.panel_electricity_upgrade)
            true
        }
        
        // Tasks button opens tasks board
        overlay.findViewById<Button>(R.id.btn_nav_tasks).setOnClickListener {
            showPanel(R.layout.panel_tasks_board)
        }
    }
    
    /**
     * Show a panel by inflating it and delegating configuration to PanelCoordinator.
     */
    private fun showPanel(layoutResId: Int) {
        val panelView = components.panelManager.showPanel(layoutResId)
        components.panelCoordinator.configurePanel(layoutResId, panelView, ::showPanel)
    }
    
    /**
     * Called when game state changes (via ObserverCoordinator).
     * Updates HUD and refreshes any visible panels.
     */
    private fun onGameStateChanged() {
        runOnUiThread {
            // Update HUD with latest state
            val stats = components.stateManager.latestStats
            if (stats != null) {
                components.hudManager.updateHud(
                    stats,
                    components.stateManager.currentActiveEvent
                )
            }
            
            // Update visible panel if any
            val currentPanel = components.panelManager.getCurrentPanel()
            val currentLayoutId = components.panelManager.getCurrentLayoutId()
            if (currentPanel != null && currentLayoutId != null) {
                components.panelCoordinator.updateVisiblePanel(currentLayoutId, currentPanel)
            }
        }
    }
}

/**
 * Reference List:
 * - Android, 2025. Android Developers Documentation. [online] Available at:
 *   https://developer.android.com/ [Accessed 1 Oct. 2025].
 * - Unity Technologies, 2025. Unity for Android. [online] Available at:
 *   https://docs.unity3d.com/Manual/android.html [Accessed 1 Oct. 2025].
 * - Google, 2025. Firebase Android SDK. [online] Available at:
 *   https://firebase.google.com/docs/android/setup [Accessed 1 Oct. 2025].
 * - Kotlin, 2025. Kotlin Standard Library. [online] Available at:
 *   https://kotlinlang.org/api/latest/jvm/stdlib/ [Accessed 1 Oct. 2025].
 */
