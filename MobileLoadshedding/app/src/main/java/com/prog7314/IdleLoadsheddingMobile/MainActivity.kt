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

class MainActivity : UnityPlayerGameActivity() { //Unity Technologies, 2025.

    // -----------------------------------------------------------------
    // Activity lifecycle contract:
    // 1. Collect references to the HUD overlay that rides on top of Unity.
    // 2. Subscribe to every Unity-driven data channel we care about.
    // 3. Fan the latest payloads across the various modal panels on demand.
    // Any deviations from this order tend to produce flicker or nulls.
    // -----------------------------------------------------------------

    // Managers - extracted to separate classes for maintainability
    private lateinit var panelManager: PanelManager
    private lateinit var hudManager: HudManager
    private lateinit var localeManager: LocaleManager
    private lateinit var formatUtils: FormatUtils
    
    // Panel Binders
    private lateinit var buildingPanelBinder: BuildingPanelBinder
    private lateinit var districtsPanelBinder: DistrictsPanelBinder
    private lateinit var electricityPanelBinder: ElectricityPanelBinder
    private lateinit var profilePanelBinder: ProfilePanelBinder
    private lateinit var settingsPanelBinder: SettingsPanelBinder
    private lateinit var leaderboardPanelBinder: LeaderboardPanelBinder
    private lateinit var friendsPanelBinder: FriendsPanelBinder
    private lateinit var tasksPanelBinder: TasksPanelBinder
    private lateinit var profileEditPanelBinder: ProfileEditPanelBinder

    private lateinit var modalContainer: FrameLayout
    private lateinit var modalScrim: View

    // Primary HUD figures that are always visible. These get refreshed
    // aggressively, so we store the references once and reuse them forever.

    private lateinit var textEnergyAmount: TextView
    private lateinit var textEnergyPerSecond: TextView
    private lateinit var textActiveGenerators: TextView
    private lateinit var textEventStatus: TextView
    private lateinit var textSafeIdleHours: TextView

    private var currentPanelLayoutId: Int? = null
    private var currentPanelView: View? = null

    // Cached payloads let us repopulate modals instantly without re-querying Unity.

    private var latestStats: UnityStatsSnapshot? = null
    private var latestLeaderboard: UnityLeaderboardPayload? = null
    private var latestFriends: FriendsPayload? = null
    private var latestSave: UnitySavePayload? = null
    private var currentActiveEvent: UnityEventPayload? = null
    private var eventHistory: List<UnityEventPayload> = emptyList()
    
    // Track previous generator stats to detect changes and trigger visual feedback.
    // WHY THIS EXISTS: When Unity sends updated stats, we need to know what changed
    // (level increase, output boost) so we can flash animations. Without this history,
    // every rebind would trigger animations even when nothing actually changed.
    // Stored as: generatorId -> Pair(previousLevel, previousOutput)
    private val previousGeneratorStats = mutableMapOf<String, Pair<Int, Double>>()

    // Numbers get formatted repeatedly—pre-compile the formatter.
    private val numberFormatter = DecimalFormat("#,##0.##")
    private val timestampFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
        .withZone(ZoneId.systemDefault())
    private val gson = Gson()
    private val auth = FirebaseAuth.getInstance()
    
    // SharedPreferences store user settings across app launches. We use a dedicated file
    // instead of default prefs to make it easier to clear game data without nuking settings.
    private lateinit var settingsPrefs: SharedPreferences
    
    // Language persistence keys. Storing index instead of locale string because:
    // 1. It's more compact (single digit vs. "af"/"zu"/etc.)
    // 2. We can validate bounds before applying (prevents crashes from corrupted prefs)
    // 3. Matches Spinner.setSelection() API exactly—no conversion needed
    companion object {
        private const val PREFS_NAME = "app_settings"
        private const val KEY_LANGUAGE_INDEX = "language_index"
        private const val DEFAULT_LANGUAGE_INDEX = 0  // English
    }

    // Unity drips fresh stats through this observer. We mirror them onto the HUD and
    // make sure every panel is resynchronized so that modal transitions stay seamless.
    private val statsObserver = object : Stats.Observer {
        override fun onStatsUpdated(snapshot: UnityStatsSnapshot) {
            runOnUiThread {
                latestStats = snapshot
                hudManager.updateHud(snapshot, currentActiveEvent)
                syncStatsPanels(snapshot)
            }
        }
    }

    // Leaderboard payloads are large, so we log the size for sanity when debugging API drift.
    private val leaderboardObserver = object : Leaderboard.Observer {
        override fun onLeaderboardUpdated(payload: UnityLeaderboardPayload) {
            Log.d("MainActivity", "Leaderboard observer notified with ${payload.entries.size} entries")
            runOnUiThread {
                latestLeaderboard = payload
                updateLeaderboardPanel()
            }
        }
    }

    // Friends list drives multiple panels (profile + dedicated friends modal), hence the cache.
    private val friendsObserver = object : FriendsList.Observer {
        override fun onFriendsUpdated(payload: FriendsPayload) {
            runOnUiThread {
                latestFriends = payload
                updateFriendsPanel()
            }
        }
    }

    // Active events determine banners, tasks, and idle safety timers in one swing.
    private val eventsObserver = object : Events.Observer {
        override fun onEventsUpdated(activeEvent: UnityEventPayload?, history: List<UnityEventPayload>) {
            runOnUiThread {
                currentActiveEvent = activeEvent
                eventHistory = history
                updateEventBanner()
                updateTasksPanel()
                syncStatsPanels(latestStats)
            }
        }
    }

    // Save payloads are rare but critical; we capture them and immediately refresh UI hints.
    private val saveListener = object : SaveSync.Listener {
        override fun onSaveUpdated(payload: UnitySavePayload) {
            runOnUiThread {
                latestSave = payload
                syncStatsPanels(latestStats)
                updateSettingsPanel()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("MainActivity", "onCreate() started")
        
        // Initialize locale manager and apply saved language
        localeManager = LocaleManager(this)
        localeManager.applySavedLocale()
        
        // Initialize settings storage
        settingsPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        
        // Step 1: Inflate the native Android overlay that sits atop the Unity surface.
        val overlay = LayoutInflater.from(this).inflate(R.layout.activity_main, null)
        modalContainer = overlay.findViewById(R.id.modal_container)
        modalScrim = overlay.findViewById(R.id.modal_scrim)

        textEnergyAmount = overlay.findViewById(R.id.text_currency_amount)
        textEnergyPerSecond = overlay.findViewById(R.id.text_energy_per_second)
        textActiveGenerators = overlay.findViewById(R.id.text_active_generators)
        textEventStatus = overlay.findViewById(R.id.text_event_status)
        textEventStatus.isVisible = false
        textSafeIdleHours = overlay.findViewById(R.id.text_safe_idle_hours)
        
        // Initialize managers and binders
        formatUtils = FormatUtils(this)
        panelManager = PanelManager(this, modalContainer, modalScrim)
        hudManager = HudManager(this, formatUtils, textEnergyAmount, textEnergyPerSecond, 
            textActiveGenerators, textEventStatus, textSafeIdleHours)
        
        districtsPanelBinder = DistrictsPanelBinder(this, formatUtils)
        electricityPanelBinder = ElectricityPanelBinder(this, formatUtils, districtsPanelBinder)
        buildingPanelBinder = BuildingPanelBinder(this, formatUtils)
        profilePanelBinder = ProfilePanelBinder(this, formatUtils, auth)
        leaderboardPanelBinder = LeaderboardPanelBinder(this, formatUtils)
        friendsPanelBinder = FriendsPanelBinder(this, formatUtils)
        tasksPanelBinder = TasksPanelBinder(this, formatUtils)
        profileEditPanelBinder = ProfileEditPanelBinder(this, auth)
        settingsPanelBinder = SettingsPanelBinder(
            this,
            auth,
            settingsPrefs,
            formatUtils,
            localeManager
        ) {
            // Clear game state callback for logout
            latestStats = null
            latestLeaderboard = null
            latestFriends = null
            latestSave = null
            currentActiveEvent = null
            eventHistory = emptyList()
            previousGeneratorStats.clear()
        }

        val layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )

        // A simple tap outside any modal is our universal close affordance.
        modalScrim.setOnClickListener { panelManager.hidePanel() }

        overlay.findViewById<ImageButton>(R.id.btn_hud_upgrades).setOnClickListener {
            // Electric upgrades live in their own template; we hot swap the modal content.
            showPanel(R.layout.panel_electricity_upgrade)
        }

        overlay.findViewById<ImageButton>(R.id.btn_hud_profile).setOnClickListener {
            // Profile is the hub for the rest of the "account" modals.
            showPanel(R.layout.panel_profile)
        }

        overlay.findViewById<Button>(R.id.btn_nav_level_up).setOnClickListener {
            // Level-up is a short hop into the building upgrades panel.
            showPanel(R.layout.panel_building_upgrade)
        }

        overlay.findViewById<Button>(R.id.btn_nav_power).setOnClickListener {
            // Quick tap for burst energy from active generators
            PowerManager.clickGenerator("generator_1")
            Stats.requestUnitySnapshot()
        }
        
        overlay.findViewById<Button>(R.id.btn_nav_power).setOnLongClickListener {
            // Holding grants direct access to power tuning—less taps during tuning sessions.
            showPanel(R.layout.panel_electricity_upgrade)
            true
        }

        overlay.findViewById<Button>(R.id.btn_nav_tasks).setOnClickListener {
            // Tasks are our live ops content; surface them instantly from the HUD.
            showPanel(R.layout.panel_tasks_board)
        }

        // Stitch the overlay on top of Unity once everything is wired up.
        addContentView(overlay, layoutParams)

        // SaveSync must boot before we query Unity, otherwise early payloads vanish into the void.
        SaveSync.initialize(this)

        window.decorView.post {
            // Once Unity is ready, hide its own HUD and request the first stats pull.
            UnityBridge.hideUnityHud()
            Stats.requestUnitySnapshot()
        }

        // Subscribe *after* the first snapshot request so late observers still pick up the initial payload.
        Stats.addObserver(statsObserver)
        Leaderboard.addObserver(leaderboardObserver)
        FriendsList.addObserver(friendsObserver)
        Events.addObserver(eventsObserver)
        SaveSync.addListener(saveListener)

    Log.d("MainActivity", "About to fetch leaderboard from API")
        
        // Fetch leaderboard directly from API instead of waiting for Unity
        Leaderboard.fetchBackendLeaderboard { result ->
            result.onSuccess { payload ->
                Log.d("MainActivity", "SUCCESS: Leaderboard fetched with ${payload.entries.size} entries")
            }.onFailure { error ->
                Log.e("MainActivity", "FAILED: Leaderboard fetch error: ${error.message}", error)
                // Fallback to Unity request if API fails
                Leaderboard.requestUnityLeaderboard()
            }
        }
        
        // Fetch friends directly from API instead of waiting for Unity
        Log.d("MainActivity", "About to fetch friends from API")
        FriendsList.fetchBackendFriends { result ->
            result.onSuccess { payload ->
                Log.d("MainActivity", "SUCCESS: Friends fetched with ${payload.friends.size} friends")
            }.onFailure { error ->
                Log.e("MainActivity", "FAILED: Friends fetch error: ${error.message}", error)
                // Fallback to Unity request if API fails
                FriendsList.requestUnityFriends()
            }
        }
        
        SaveSync.requestUnitySave()
        
        Log.d("MainActivity", "onCreate() completed")
    }

    override fun onDestroy() {
        // Remember to clean up every observer we registered in onCreate; Unity will happily
        // keep dispatching otherwise, leaking references back into this activity.
        Stats.removeObserver(statsObserver)
        Leaderboard.removeObserver(leaderboardObserver)
        FriendsList.removeObserver(friendsObserver)
        Events.removeObserver(eventsObserver)
        SaveSync.removeListener(saveListener)
        super.onDestroy()
    }

    private fun showPanel(layoutResId: Int) {
        val panelView = panelManager.showPanel(layoutResId)

        // Each layout has unique wiring requirements—dispatch to the dedicated configurator.
        when (layoutResId) {
            R.layout.panel_electricity_upgrade -> configureElectricityPanel(panelView)
            R.layout.panel_building_upgrade -> configureBuildingPanel(panelView)
            R.layout.panel_profile -> configureProfilePanel(panelView)
            R.layout.panel_profile_edit -> configureProfileEditPanel(panelView)
            R.layout.panel_leaderboard -> configureLeaderboardPanel(panelView)
            R.layout.panel_settings -> configureSettingsPanel(panelView)
            R.layout.panel_friends_leaderboard -> configureFriendsLeaderboardPanel(panelView)
            R.layout.panel_tasks_board -> configureTasksPanel(panelView)
        }
    }

    private fun hidePanel() = panelManager.hidePanel()

    private fun configureElectricityPanel(panel: View) = 
        electricityPanelBinder.configure(
            panel, 
            latestStats, 
            latestSave, 
            currentActiveEvent, 
            eventHistory
        ) { layoutId -> showPanel(layoutId) }

    private fun configureBuildingPanel(panel: View) = 
        buildingPanelBinder.configure(panel, latestStats) { layoutId -> showPanel(layoutId) }

    private fun configureProfilePanel(panel: View) = 
        profilePanelBinder.configure(panel, latestStats, latestSave) { layoutId -> showPanel(layoutId) }

    private fun configureProfileEditPanel(panel: View) = 
        profileEditPanelBinder.configure(panel) { layoutId -> showPanel(layoutId) }

    // Settings panel handles both save management and app-wide preferences (language, theme).
    // Language switching is tricky because:
    // 1. Locale changes require activity recreation to fully apply
    // 2. We need to persist the choice so it survives app restarts
    // 3. Spinner selection must match saved preference on first load
    // 4. Android 13+ requires different locale APIs than older versions
    private fun configureSettingsPanel(panel: View) = 
        settingsPanelBinder.configure(panel, latestSave)

    private fun configureLeaderboardPanel(panel: View) = 
        leaderboardPanelBinder.configure(panel, latestLeaderboard) { layoutId -> showPanel(layoutId) }

    private fun configureFriendsLeaderboardPanel(panel: View) = 
        friendsPanelBinder.configure(panel, latestFriends) { layoutId -> showPanel(layoutId) }

    private fun configureTasksPanel(panel: View) = 
        tasksPanelBinder.configure(panel, eventHistory, currentActiveEvent)

    private fun updateHud(snapshot: UnityStatsSnapshot) {
        // Credits prefer money, then energy, then lifetime generated output—mirrors Unity HUD logic.
        val credits = when {
            snapshot.currentMoney > 0.0 -> snapshot.currentMoney
            snapshot.currentEnergy > 0.0 -> snapshot.currentEnergy
            else -> snapshot.totalEnergyGenerated
        }
        textEnergyAmount.text = formatEnergy(credits)

        textEnergyPerSecond.text = getString(
            R.string.hud_energy_per_second_format,
            formatEnergy(snapshot.energyPerSecond)
        )

        textActiveGenerators.text = getString(
            R.string.hud_generators_format,
            snapshot.activeGenerators,
            snapshot.generatorLevelSum
        )

        val safeSeconds = when {
            snapshot.hoursUntilMaintenance > 0.0 -> snapshot.hoursUntilMaintenance * 3600.0
            snapshot.offlineSeconds > 0.0 -> snapshot.offlineSeconds
            else -> 0.0
        }
        val safeIdle = if (safeSeconds > 0.0) formatDuration(safeSeconds) else getString(R.string.timestamp_unknown)
        textSafeIdleHours.text = getString(R.string.hud_safe_idle_format, safeIdle)

        // Banner relies on stats-derived event state, so refresh it after every HUD update.
        updateEventBanner()
    }

    private fun updateEventBanner() {
        val event = currentActiveEvent
        if (event != null) {
            textEventStatus.isVisible = true
            textEventStatus.text = getString(R.string.hud_event_active, event.title, event.productionMultiplier)
        } else {
            textEventStatus.isVisible = false
            textEventStatus.text = getString(R.string.hud_event_inactive)
        }
    }

    private fun syncStatsPanels(snapshot: UnityStatsSnapshot?) {
        val panelView = currentPanelView ?: return
        // Only touch the panel currently on screen; everything else will rebind on open.
        when (currentPanelLayoutId) {
            R.layout.panel_electricity_upgrade -> 
                electricityPanelBinder.bindData(panelView, snapshot, latestSave, currentActiveEvent, eventHistory)
            R.layout.panel_profile -> 
                profilePanelBinder.bindData(panelView, snapshot, latestSave)
            R.layout.panel_building_upgrade -> 
                buildingPanelBinder.bindData(panelView, snapshot)
        }
    }

    // Districts are the core economic units—each row tracks power output, efficiency decay,
    // and maintenance timers. This panel gets hammered during tuning sessions, so we batch
    // all the view inflation and binding to minimize jank.
    private fun renderDistrictsPanel(panel: View, stats: UnityStatsSnapshot?) {
        val container = panel.findViewById<LinearLayout?>(R.id.list_districts) ?: return
        container.removeAllViews()

        val districts = stats?.districts.orEmpty()
        if (districts.isEmpty()) {
            // Density-aware padding keeps the empty state centered on all form factors.
            val padding = (panel.resources.displayMetrics.density * 12).toInt()
            val emptyView = TextView(panel.context).apply {
                setText(R.string.districts_empty)
                textAlignment = View.TEXT_ALIGNMENT_CENTER
                setPadding(0, padding, 0, padding)
            }
            container.addView(emptyView)
            return
        }

        // Reuse a single inflater instance across all rows—minor optimization, but adds up.
        val inflater = LayoutInflater.from(panel.context)
        districts.forEach { district ->
            val row = inflater.inflate(R.layout.item_district_status, container, false)
            row.findViewById<TextView?>(R.id.text_district_name)?.text = district.name
            row.findViewById<TextView?>(R.id.text_district_level)?.text =
                getString(R.string.district_level_format, district.level)

            // Districts produce both power and money—display them side-by-side for quick scanning.
            val powerText = getString(
                R.string.district_output_format,
                formatEnergy(district.powerPerSecond),
                formatEnergy(district.moneyPerSecond)
            )
            row.findViewById<TextView?>(R.id.text_district_output)?.text = powerText

            // Efficiency is a 0–1 float from Unity; we clamp and scale it to a percentage.
            // Clamping prevents weird UI if Unity sends bad data during dev builds.
            val efficiencyPercent = (district.efficiency * 100).roundToInt().coerceIn(0, 100)
            row.findViewById<TextView?>(R.id.text_district_efficiency)?.text =
                getString(R.string.district_efficiency_format, efficiencyPercent)

            // Unity gives hours, we need seconds—multiply by 3600 and clamp negatives.
            val idleSeconds = (district.hoursUntilMaintenance * 3600).coerceAtLeast(0.0)
            val idleText = if (idleSeconds <= 0.0) {
                getString(R.string.timestamp_unknown)
            } else {
                formatDuration(idleSeconds)
            }
            row.findViewById<TextView?>(R.id.text_district_idle_time)?.text =
                getString(R.string.district_idle_time_format, idleText)

            // Service button resets maintenance timers; always enabled for UX consistency.
            row.findViewById<Button?>(R.id.btn_district_service)?.setOnClickListener {
                PowerManager.serviceDistrict(district.id)
                Stats.requestUnitySnapshot()
            }

            // Upgrade button toggles based on whether district is maxed or affordable.
            // Zero/negative cost is Unity's signal that no more upgrades exist.
            val upgradeButton = row.findViewById<Button?>(R.id.btn_district_upgrade)
            if (district.nextUpgradeCost <= 0.0) {
                upgradeButton?.isEnabled = false
                upgradeButton?.text = getString(R.string.district_upgrade_maxed)
            } else {
                upgradeButton?.isEnabled = true
                upgradeButton?.text = getString(
                    R.string.district_upgrade_cost_format,
                    formatEnergy(district.nextUpgradeCost)
                )
                upgradeButton?.setOnClickListener {
                    PowerManager.upgradeDistrict(district.id)
                    Stats.requestUnitySnapshot()
                }
            }

            container.addView(row)
        }
    }

    // Leaderboard has three distinct visual states: loading, empty, and populated.
    // We toggle visibility on each state container to avoid recreating views mid-fetch.
    // Delegate panel updates to binders
    private fun updateLeaderboardPanel() {
        if (currentPanelLayoutId == R.layout.panel_leaderboard) {
            currentPanelView?.let { leaderboardPanelBinder.bindData(it, latestLeaderboard) }
        }
    }

    private fun updateFriendsPanel() {
        if (currentPanelLayoutId == R.layout.panel_friends_leaderboard) {
            currentPanelView?.let { friendsPanelBinder.bindData(it, latestFriends) }
        }
    }

    private fun updateSettingsPanel() {
        if (currentPanelLayoutId == R.layout.panel_settings) {
            currentPanelView?.let { settingsPanelBinder.configure(it, latestSave) }
        }
    }

    private fun updateTasksPanel() {
        if (currentPanelLayoutId == R.layout.panel_tasks_board) {
            currentPanelView?.let { tasksPanelBinder.configure(it, eventHistory, currentActiveEvent) }
        }
    }

    // Delegate to FormatUtils
    private fun formatEnergy(value: Double): String = formatUtils.formatEnergy(value)
    private fun formatDuration(seconds: Double): String = formatUtils.formatDuration(seconds)
    private fun formatTimestamp(isoString: String?): String = formatUtils.formatTimestamp(isoString)

    // Event descriptions get displayed in multiple contexts (banner, tasks panel, etc.).
    // Standardize the format here: state + multiplier, separated by bullet for visual clarity.
    private fun formatEventDescription(event: UnityEventPayload): String {
        val state = if (event.isStarting) getString(R.string.tasks_event_active, event.title) else getString(
            R.string.tasks_event_inactive,
            event.title
        )
        val multiplier = getString(R.string.tasks_event_multiplier, event.productionMultiplier)
        // Bullet separator mirrors the pattern used in friends/tasks detail lines.
        return "$state • $multiplier"
    }

    // District actions (service, upgrade) are sent to Unity via JSON payloads.
    // The blank-check prevents accidental nulls from crashing the Unity message router.
    // UnityBridge.sendJsonToUnity expects: (gameObjectName, methodName, jsonPayload).
    private fun sendDistrictAction(methodName: String, districtId: String) {
        if (districtId.isBlank()) return
        val payload = gson.toJson(DistrictAction(districtId)) //Google, 2025.
        // "UnityMessageRouter" is the GameObject name in the Unity scene; hardcoded for now.
        // TODO: Consider externalizing this if we ever support multiple Unity scenes.
        UnityBridge.sendJsonToUnity("UnityMessageRouter", methodName, payload)
    }

    // Simple data class for district actions; Gson serializes this into {"districtId":"..."}
    private data class DistrictAction(val districtId: String)
    
    // ==================================================================================
    // LOCALE MANAGEMENT UTILITIES
    // ==================================================================================
    // These methods handle language switching across the entire app. This was one of the
    // most frustrating features to implement because:
    // 1. Android's locale APIs changed drastically in API 24+ (Nougat)
    // 2. Configuration updates don't automatically propagate to all contexts
    // 3. String resources get cached aggressively—partial updates cause mixed languages
    // 4. Unity bridge needs special handling to avoid passing stale localized strings
    // ==================================================================================
    
    // Delegate to LocaleManager
    private fun applySavedLocale() = localeManager.applySavedLocale()
    private fun applyLocale(languageIndex: Int) = localeManager.applyLocale(languageIndex)
}

/**
 * Reference List:
 * - Android, 2025. Android Developers Documentation. [online] Available at:
 *   https://developer.android.com/ [Accessed 1 Oct. 2025].
 * - AndroidX, 2025. AndroidX Core Library. [online] Available at:
 *   https://developer.android.com/jetpack/androidx/releases/core [Accessed 1 Oct. 2025].
 * - Google, 2025. Gson - A Java serialization library. [online] Available at:
 *   https://github.com/google/gson [Accessed 1 Oct. 2025].
 * - Unity Technologies, 2025. Unity for Android. [online] Available at:
 *   https://docs.unity3d.com/Manual/android.html [Accessed 1 Oct. 2025].
 * - Oracle, 2025. Java Platform Standard Edition Documentation. [online] Available at:
 *   https://docs.oracle.com/en/java/javase/ [Accessed 1 Oct. 2025].
 * - Kotlin, 2025. Kotlin Standard Library. [online] Available at:
 *   https://kotlinlang.org/api/latest/jvm/stdlib/ [Accessed 1 Oct. 2025].
 */
