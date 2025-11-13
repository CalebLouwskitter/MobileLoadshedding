package com.prog7314.IdleLoadsheddingMobile

import android.content.ClipData //Android, 2025.
import android.content.ClipboardManager //Android, 2025.
import android.content.Context //Android, 2025.
import android.content.Intent //Android, 2025.
import android.content.SharedPreferences //Android, 2025.
import android.content.res.Configuration //Android, 2025.
import android.os.Bundle //Android, 2025.
import android.util.Log //Android, 2025.
import android.view.LayoutInflater //Android, 2025.
import android.view.View //Android, 2025.
import android.widget.AdapterView //Android, 2025.
import android.widget.Button //Android, 2025.
import android.widget.EditText //Android, 2025.
import android.widget.FrameLayout //Android, 2025.
import android.widget.Spinner //Android, 2025.
import android.widget.ImageButton //Android, 2025.
import android.widget.ImageView //Android, 2025.
import android.widget.LinearLayout //Android, 2025.
import android.widget.ProgressBar //Android, 2025.
import android.widget.TextView //Android, 2025.
import android.widget.Toast //Android, 2025.
import androidx.core.view.isVisible //AndroidX, 2025.
import com.google.firebase.auth.FirebaseAuth //Firebase, 2025.
import com.google.gson.Gson //Google, 2025.
import com.prog7314.IdleLoadsheddingMobile.core.UnityBridge
import com.prog7314.IdleLoadsheddingMobile.data.models.*
import com.prog7314.IdleLoadsheddingMobile.unity.Events
import com.prog7314.IdleLoadsheddingMobile.unity.FriendsList
import com.prog7314.IdleLoadsheddingMobile.unity.Leaderboard
import com.prog7314.IdleLoadsheddingMobile.unity.PowerManager
import com.prog7314.IdleLoadsheddingMobile.unity.SaveSync
import com.prog7314.IdleLoadsheddingMobile.unity.Stats
// TEMPORARILY USING STUB - Replace with real Unity import after refactoring
// import com.unity3d.player.UnityPlayerGameActivity //Unity Technologies, 2025.
import com.prog7314.IdleLoadsheddingMobile.stub.UnityPlayerGameActivity
import com.prog7314.IdleLoadsheddingMobile.ui.HudManager
import com.prog7314.IdleLoadsheddingMobile.ui.PanelManager
import com.prog7314.IdleLoadsheddingMobile.ui.coordinators.UIComponentFactory
import com.prog7314.IdleLoadsheddingMobile.ui.panels.*
import com.prog7314.IdleLoadsheddingMobile.utils.FormatUtils
import com.prog7314.IdleLoadsheddingMobile.utils.LocaleManager
import java.text.DecimalFormat //Oracle, 2025.
import java.time.Instant //Oracle, 2025.
import java.time.ZoneId //Oracle, 2025.
import java.util.Locale //Oracle, 2025.
import java.time.format.DateTimeFormatter //Oracle, 2025.
import java.time.format.DateTimeParseException //Oracle, 2025.
import kotlin.math.roundToInt //Kotlin, 2025.

class MainActivity : UnityPlayerGameActivity() {

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
