package com.prog7314.IdleLoadsheddingMobile

import android.content.ClipData //Android, 2025.
import android.content.ClipboardManager //Android, 2025.
import android.content.Context //Android, 2025.
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
import com.unity3d.player.UnityPlayerGameActivity //Unity Technologies, 2025.
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
                updateHud(snapshot)
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
        
        // Initialize settings storage before anything else—locale changes affect string resources.
        settingsPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        
        // Apply saved language immediately. This MUST happen before view inflation or we get
        // a mix of old/new language strings during the transition (looks terrible).
        applySavedLocale()
        
        // Step 1: Inflate the native Android overlay that sits atop the Unity surface.
        // Doing this before touching Unity keeps view hierarchy mutations predictable.
        val overlay = LayoutInflater.from(this).inflate(R.layout.activity_main, null)
        modalContainer = overlay.findViewById(R.id.modal_container)
        modalScrim = overlay.findViewById(R.id.modal_scrim)

        textEnergyAmount = overlay.findViewById(R.id.text_currency_amount)
        textEnergyPerSecond = overlay.findViewById(R.id.text_energy_per_second)
        textActiveGenerators = overlay.findViewById(R.id.text_active_generators)
        textEventStatus = overlay.findViewById(R.id.text_event_status)
        textEventStatus.isVisible = false
    textSafeIdleHours = overlay.findViewById(R.id.text_safe_idle_hours)

        val layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )

        // A simple tap outside any modal is our universal close affordance.
        modalScrim.setOnClickListener { hidePanel() }

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
        // We inflate a fresh copy each time to avoid stale state bleeding between panels.
        val panelView = LayoutInflater.from(this).inflate(layoutResId, modalContainer, false)
        modalContainer.removeAllViews()
        modalContainer.addView(panelView)
        modalContainer.visibility = View.VISIBLE
        modalScrim.visibility = View.VISIBLE

        currentPanelLayoutId = layoutResId
        currentPanelView = panelView

        panelView.findViewById<View?>(R.id.close_button)?.setOnClickListener { hidePanel() }

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

    private fun hidePanel() {
        // Tear the modal down entirely so the next showPanel call starts from a clean slate.
        modalContainer.removeAllViews()
        modalContainer.visibility = View.GONE
        modalScrim.visibility = View.GONE
        currentPanelLayoutId = null
        currentPanelView = null
    }

    private fun configureElectricityPanel(panel: View) {
        // Tabs share the same modal container; swap layouts instead of fragments to stay light-weight.
        panel.findViewById<View?>(R.id.tab_building)?.setOnClickListener {
            showPanel(R.layout.panel_building_upgrade)
        }

        panel.findViewById<Button?>(R.id.btn_refresh_production)?.setOnClickListener {
            // Force Unity to hand us the latest production and save metrics.
            Stats.requestUnitySnapshot()
            SaveSync.requestUnitySave()
        }

        bindElectricityPanel(panel, latestStats, latestSave, currentActiveEvent)
    }

    private fun configureBuildingPanel(panel: View) {
        // Mirror the tab behavior so users can bounce between power/building quickly.
        panel.findViewById<View?>(R.id.tab_electricity)?.setOnClickListener {
            showPanel(R.layout.panel_electricity_upgrade)
        }
        
        bindBuildingPanel(panel, latestStats)
    }

    private fun configureProfilePanel(panel: View) {
        panel.findViewById<View?>(R.id.tab_leaderboard)?.setOnClickListener {
            showPanel(R.layout.panel_leaderboard)
        }

        panel.findViewById<View?>(R.id.btn_profile_edit)?.setOnClickListener {
            showPanel(R.layout.panel_profile_edit)
        }

        panel.findViewById<View?>(R.id.btn_profile_settings)?.setOnClickListener {
            showPanel(R.layout.panel_settings)
        }

        panel.findViewById<View?>(R.id.btn_profile_friends)?.setOnClickListener {
            // Friends leaderboard lives in its own layout to keep adapters simple.
            showPanel(R.layout.panel_friends_leaderboard)
        }

        panel.findViewById<View?>(R.id.btn_profile_logout)?.setOnClickListener {
            Toast.makeText(this, getString(R.string.toast_logout_placeholder), Toast.LENGTH_SHORT).show()
            hidePanel()
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
            // Let people invite via chat apps quickly—copy straight to clipboard.
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Friend Code", friendCode)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, getString(R.string.toast_friend_code_copied), Toast.LENGTH_SHORT).show()
        }

        // Set cosmetic tag based on stats
        val tag = panel.findViewById<TextView?>(R.id.text_profile_tag)
        latestStats?.let { stats ->
            when {
                stats.totalEnergyGenerated > 1000000.0 -> {
                    tag?.text = "🏆 VETERAN"
                    tag?.setBackgroundColor(0xFFFFD700.toInt())
                }
                stats.totalEnergyGenerated > 10000.0 -> {
                    tag?.text = "⚡ ACTIVE"
                    tag?.setBackgroundColor(0xFFFFC107.toInt())
                }
                else -> {
                    tag?.text = "🌟 NEWBIE"
                    tag?.setBackgroundColor(0xFF4CAF50.toInt())
                }
            }
        }

        bindProfilePanel(panel, latestStats, latestSave)
    }

    private fun configureProfileEditPanel(panel: View) {
        val user = auth.currentUser
        
        // Populate current nickname and email
        panel.findViewById<EditText?>(R.id.input_nickname)?.setText(user?.displayName ?: "")
        panel.findViewById<TextView?>(R.id.text_email_static)?.text = user?.email ?: "Not logged in"
        
        panel.findViewById<Button?>(R.id.btn_cancel_profile)?.setOnClickListener {
            // Cancelling takes you back to the profile summary without a transition.
            showPanel(R.layout.panel_profile)
        }

        panel.findViewById<Button?>(R.id.btn_save_profile)?.setOnClickListener {
            val nicknameInput = panel.findViewById<EditText?>(R.id.input_nickname)
            val newNickname = nicknameInput?.text?.toString()?.trim()
            
            if (!newNickname.isNullOrEmpty()) {
                // Update Firebase user profile with new nickname
                val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                    .setDisplayName(newNickname)
                    .build()
                
                user?.updateProfile(profileUpdates)?.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, getString(R.string.toast_profile_updated), Toast.LENGTH_SHORT).show()
                        showPanel(R.layout.panel_profile)
                    } else {
                        // Firebase may return network/auth errors—surfacing a generic toast keeps UX simple.
                        Toast.makeText(this, getString(R.string.toast_profile_update_failed), Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, getString(R.string.toast_nickname_empty), Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Settings panel handles both save management and app-wide preferences (language, theme).
    // Language switching is tricky because:
    // 1. Locale changes require activity recreation to fully apply
    // 2. We need to persist the choice so it survives app restarts
    // 3. Spinner selection must match saved preference on first load
    // 4. Android 13+ requires different locale APIs than older versions
    private fun configureSettingsPanel(panel: View) {
        // Upload save button
        panel.findViewById<Button?>(R.id.btn_upload_save)?.setOnClickListener {
            // Manual upload path—players expect immediate feedback, hence the toast.
            SaveSync.requestUnitySave()
            Toast.makeText(this, getString(R.string.toast_save_uploaded), Toast.LENGTH_SHORT).show()
        }

        // Sync save button
        panel.findViewById<Button?>(R.id.btn_sync_save)?.setOnClickListener {
            // This variant also refreshes stats to capture the pre/post delta.
            Stats.requestUnitySnapshot()
            SaveSync.requestUnitySave()
            Toast.makeText(this, getString(R.string.toast_save_synced), Toast.LENGTH_SHORT).show()
        }

        // Display last sync time
        val lastSyncText = panel.findViewById<TextView?>(R.id.text_last_save_sync)
        latestSave?.let { save ->
            // Timestamp arrives in UTC ISO string; formatTimestamp does the heavy lifting.
            lastSyncText?.text = "Last synced: ${formatTimestamp(save.timestampUtc)}"
        }
        
        // Language spinner setup—this was surprisingly painful to get right.
        // Key issues we had to solve:
        // - Spinner fires onItemSelected during initial setSelection(), causing unwanted saves
        // - Need to distinguish user taps from programmatic selection
        // - Locale codes array must stay in sync with display names array
        val languageSpinner = panel.findViewById<Spinner?>(R.id.spinner_language)
        languageSpinner?.let { spinner ->
            // Restore previously saved language selection
            val savedIndex = settingsPrefs.getInt(KEY_LANGUAGE_INDEX, DEFAULT_LANGUAGE_INDEX)
            
            // Validate saved index to prevent crashes from corrupted prefs or array changes.
            // Clamp to valid range rather than resetting to default—less jarring for users.
            val languageCount = resources.getStringArray(R.array.settings_languages).size
            val safeIndex = savedIndex.coerceIn(0, languageCount - 1)
            
            // Flag to ignore the initial onItemSelected callback that fires during setSelection().
            // Without this, opening settings would trigger an unwanted locale change.
            var isInitialSelection = true
            
            spinner.setSelection(safeIndex)
            
            spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    // Skip the callback that fires when we programmatically set selection above.
                    if (isInitialSelection) {
                        isInitialSelection = false
                        return
                    }
                    
                    // User actually changed the language—persist and apply it.
                    if (position != savedIndex) {
                        Log.d("MainActivity", "Language changed from index $savedIndex to $position")
                        
                        // Save preference immediately so it persists across app restarts.
                        settingsPrefs.edit().putInt(KEY_LANGUAGE_INDEX, position).apply()
                        
                        // Apply the new locale. This updates the configuration but doesn't
                        // automatically refresh existing views—that's why we recreate().
                        applyLocale(position)
                        
                        // Show feedback toast BEFORE recreation. The toast survives the transition
                        // and provides visual confirmation that the change is processing.
                        Toast.makeText(
                            this@MainActivity,
                            getString(R.string.settings_language_changed),
                            Toast.LENGTH_LONG
                        ).show()
                        
                        // Recreate activity to apply locale change to all views/strings.
                        // This is the nuclear option, but it's the only reliable way to ensure
                        // every TextView, Toast, and string resource reflects the new language.
                        // We tried partial updates (re-inflating panels, etc.) but edge cases
                        // kept slipping through—cached strings in Unity bridge, half-translated
                        // toasts, etc. Full recreation is slower but bulletproof.
                        recreate()
                    }
                }
                
                override fun onNothingSelected(parent: AdapterView<*>?) {
                    // Spinner always has a selection, so this never fires in practice.
                    // Required by interface but left empty.
                }
            }
        }
        
        // Theme spinner—wired up but not yet functional. TODO for future sprint.
        // Keeping the UI in place so users know it's coming.
        val themeSpinner = panel.findViewById<Spinner?>(R.id.spinner_theme)
        themeSpinner?.setSelection(0)  // Always default to Light for now
    }

    private fun configureLeaderboardPanel(panel: View) {
        Log.d("MainActivity", "Configuring leaderboard panel")
        
        panel.findViewById<View?>(R.id.btn_tab_friends)?.setOnClickListener {
            // Friend tab points to a separate layout so that adapters remain scoped and lazy.
            showPanel(R.layout.panel_friends_leaderboard)
        }

        panel.findViewById<View?>(R.id.btn_tab_global)?.setOnClickListener {
            Log.d("MainActivity", "Global tab clicked - fetching from API")
            Leaderboard.fetchBackendLeaderboard { result ->
                result.onFailure { error ->
                    Log.e("MainActivity", "Failed to refresh leaderboard: ${error.message}", error)
                }
            }
        }

        bindLeaderboardPanel(panel, latestLeaderboard)
        if (latestLeaderboard == null) {
            Log.d("MainActivity", "No cached leaderboard data, fetching from API")
            Leaderboard.fetchBackendLeaderboard { result ->
                result.onFailure { error ->
                    Log.e("MainActivity", "Failed to fetch initial leaderboard: ${error.message}", error)
                }
            }
        } else {
            Log.d("MainActivity", "Using cached leaderboard data with ${latestLeaderboard?.entries?.size} entries")
        }
    }

    private fun configureFriendsLeaderboardPanel(panel: View) {
        Log.d("MainActivity", "Configuring friends leaderboard panel")
        
        panel.findViewById<TextView?>(R.id.btn_show_global)?.setOnClickListener {
            // Bounce users back to the global leaderboard without reconstructing adapters.
            showPanel(R.layout.panel_leaderboard)
        }

        panel.findViewById<TextView?>(R.id.btn_show_friends)?.setOnClickListener {
            Log.d("MainActivity", "Refreshing friends list from backend")
            FriendsList.fetchBackendFriends { result ->
                result.onSuccess { payload ->
                    Log.d("MainActivity", "SUCCESS: Friends fetched with ${payload.friends.size} entries")
                }.onFailure { error ->
                    Log.e("MainActivity", "FAILED: Friends fetch error: ${error.message}", error)
                    // We surface a toast here because friend refreshes are an explicit user action.
                    Toast.makeText(this, "Failed to refresh friends list", Toast.LENGTH_SHORT).show()
                }
            }
        }

        panel.findViewById<Button?>(R.id.btn_add_friend)?.setOnClickListener {
            val codeInput = panel.findViewById<EditText?>(R.id.input_friend_code)
            val friendCode = codeInput?.text?.toString()?.trim().orEmpty()
            if (friendCode.isEmpty()) {
                Toast.makeText(this, getString(R.string.toast_friend_code_empty), Toast.LENGTH_SHORT).show()
            } else {
                FriendsList.updateFriend(
                    friendCode,
                    getString(R.string.friend_placeholder_name, friendCode),
                    15.0,
                    true
                )
                Toast.makeText(this, getString(R.string.toast_friend_invite_sent), Toast.LENGTH_SHORT).show()
                codeInput?.text?.clear()
                
                // Refresh friends list after adding
                FriendsList.fetchBackendFriends { result ->
                    result.onFailure { error ->
                        Log.e("MainActivity", "Failed to refresh after adding friend: ${error.message}", error)
                    }
                }
            }
        }

        bindFriendsPanel(panel, latestFriends)
        
        // Fetch from backend on initial load
        if (latestFriends == null) {
            Log.d("MainActivity", "No cached friends data, fetching from backend API")
            FriendsList.fetchBackendFriends { result ->
                result.onSuccess { payload ->
                    Log.d("MainActivity", "Initial friends fetch successful: ${payload.friends.size} friends")
                }.onFailure { error ->
                    Log.e("MainActivity", "Failed to fetch initial friends: ${error.message}", error)
                    // Fallback to Unity request if API fails
                    FriendsList.requestUnityFriends()
                }
            }
        } else {
            Log.d("MainActivity", "Using cached friends data with ${latestFriends?.friends?.size} entries")
        }
    }

    private fun configureTasksPanel(panel: View) {
        // The tasks panel is pure data binding; we route through updateTasksPanel for consistency.
        updateTasksPanel()
    }

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
            R.layout.panel_electricity_upgrade -> bindElectricityPanel(panelView, snapshot, latestSave, currentActiveEvent)
            R.layout.panel_profile -> bindProfilePanel(panelView, snapshot, latestSave)
            R.layout.panel_building_upgrade -> bindBuildingPanel(panelView, snapshot)
        }
    }

    private fun bindElectricityPanel(
        panel: View,
        stats: UnityStatsSnapshot?,
        save: UnitySavePayload?,
        event: UnityEventPayload?
    ) {
        val currentEnergy = stats?.let { getString(R.string.electricity_current_energy, formatEnergy(it.currentEnergy)) }
            ?: getString(R.string.electricity_current_energy, getString(R.string.timestamp_unknown))
        val production = stats?.let {
            getString(R.string.electricity_production, formatEnergy(it.energyPerSecond))
        } ?: getString(R.string.electricity_production, getString(R.string.timestamp_unknown))
        val generators = stats?.let {
            getString(R.string.electricity_generators, it.activeGenerators, it.generatorLevelSum)
        } ?: getString(R.string.electricity_generators, 0, 0)
        val currency = stats?.let {
            getString(R.string.electricity_currency, formatEnergy(it.currentMoney))
        } ?: getString(R.string.electricity_currency, getString(R.string.timestamp_unknown))
        val offline = stats?.let {
            getString(R.string.electricity_offline, formatDuration(it.offlineSeconds))
        } ?: getString(R.string.electricity_offline, getString(R.string.timestamp_unknown))
        val safeIdle = stats?.let {
            val seconds = (it.hoursUntilMaintenance * 3600).coerceAtLeast(0.0)
            getString(R.string.electricity_safe_idle, if (seconds <= 0.0) getString(R.string.timestamp_unknown) else formatDuration(seconds))
        } ?: getString(R.string.electricity_safe_idle, getString(R.string.timestamp_unknown))
        val coreCharge = stats?.let {
            val percent = (it.coreChargePercent * 100).roundToInt().coerceIn(0, 100)
            getString(R.string.electricity_core_charge, percent)
        } ?: getString(R.string.electricity_core_charge, 0)
        val lastEvent = eventHistory.firstOrNull()?.let {
            getString(R.string.electricity_last_event, formatEventDescription(it))
        } ?: getString(R.string.electricity_last_event, getString(R.string.hud_event_inactive))
        val lastSave = save?.let {
            getString(R.string.electricity_last_save, formatTimestamp(it.timestampUtc))
        } ?: getString(R.string.electricity_last_save, getString(R.string.timestamp_unknown))

        // Bind every metric into its corresponding stub view—panels get inflated without data.
        panel.findViewById<TextView?>(R.id.text_current_energy_value)?.text = currentEnergy
        panel.findViewById<TextView?>(R.id.text_current_production_value)?.text = production
        panel.findViewById<TextView?>(R.id.text_generators_value)?.text = generators
        panel.findViewById<TextView?>(R.id.text_currency_value)?.text = currency
        panel.findViewById<TextView?>(R.id.text_offline_time_value)?.text = offline
        panel.findViewById<TextView?>(R.id.text_safe_idle_value)?.text = safeIdle
        panel.findViewById<TextView?>(R.id.text_core_charge_value)?.text = coreCharge
        panel.findViewById<TextView?>(R.id.text_last_event_value)?.text = lastEvent
        panel.findViewById<TextView?>(R.id.text_last_save_value)?.text = lastSave

        renderDistrictsPanel(panel, stats)
    }

    private fun bindProfilePanel(
        panel: View,
        stats: UnityStatsSnapshot?,
        save: UnitySavePayload?
    ) {
        panel.findViewById<TextView?>(R.id.text_profile_energy_total)?.text = stats?.let {
            getString(R.string.profile_energy_total, formatEnergy(it.totalEnergyGenerated))
        } ?: getString(R.string.profile_energy_total, getString(R.string.timestamp_unknown))

        panel.findViewById<TextView?>(R.id.text_profile_energy_per_second)?.text = stats?.let {
            getString(R.string.profile_energy_per_second, formatEnergy(it.energyPerSecond))
        } ?: getString(R.string.profile_energy_per_second, getString(R.string.timestamp_unknown))

        panel.findViewById<TextView?>(R.id.text_profile_generators)?.text = stats?.let {
            getString(R.string.profile_generators_count, it.activeGenerators)
        } ?: getString(R.string.profile_generators_count, 0)

        panel.findViewById<TextView?>(R.id.text_profile_offline)?.text = stats?.let {
            getString(R.string.profile_offline_time, formatDuration(it.offlineSeconds))
        } ?: getString(R.string.profile_offline_time, getString(R.string.timestamp_unknown))

        panel.findViewById<TextView?>(R.id.text_profile_last_sync)?.text = save?.let {
            getString(R.string.profile_last_sync, formatTimestamp(it.timestampUtc))
        } ?: getString(R.string.profile_last_sync, getString(R.string.timestamp_unknown))
    }

    // Building panel shows generator upgrades. This was tricky because Unity doesn't send
    // per-generator level data—only totals (activeGenerators + generatorLevelSum). So we:
    // 1. Calculate average level across all generators
    // 2. Show estimated output based on total production divided by generator count
    // 3. Request fresh stats after every upgrade to show changes immediately
    // 4. DETECT CHANGES and trigger visual feedback (flash, highlight, +1 indicator)
    // 
    // Ideally Unity would send: generators: [{id:"gen_1", level:5}, {id:"gen_2", level:3}]
    // But restructuring that payload would break existing save compatibility, so we work
    // around it by showing aggregated stats and making sure updates are fast + VISIBLE.
    private fun bindBuildingPanel(panel: View, stats: UnityStatsSnapshot?) {
        val container = panel.findViewById<LinearLayout?>(R.id.list_building_upgrades)
        container?.removeAllViews()

        if (stats == null) {
            // Without stats we can't estimate levels—surface a friendly loading message.
            val emptyText = TextView(this)
            emptyText.text = "Loading generator data..."
            emptyText.setTextColor(0xFF052E45.toInt())
            emptyText.textSize = 14f
            emptyText.setPadding(16, 16, 16, 16)
            container?.addView(emptyText)
            return
        }

        // Create upgrade rows for each potential generator (up to 4 generators in the scene).
        // We display all 4 slots even if some aren't active yet—this shows progression path.
        val generatorIds = listOf("generator_1", "generator_2", "generator_3", "generator_4")
        
        // Calculate average level from aggregated data. This updates in real-time when
        // user taps upgrade because we request a fresh snapshot after each upgrade.
        val avgLevel = if (stats.activeGenerators > 0) {
            stats.generatorLevelSum / stats.activeGenerators
        } else {
            1  // Default to level 1 if no generators are active yet
        }
        
        generatorIds.forEachIndexed { index, genId ->
            val generatorView = LayoutInflater.from(this).inflate(R.layout.view_upgrade_item, container, false)
            
            // Generator name is static—just "Generator 1", "Generator 2", etc.
            generatorView.findViewById<TextView?>(R.id.text_upgrade_name)?.text = "Generator ${index + 1}"
            
            // Estimate output by dividing total production equally among active generators.
            // This isn't perfectly accurate (generators might have different multipliers), but
            // it's the best we can do with aggregated stats. Better than showing nothing.
            val estimatedOutput = if (stats.activeGenerators > 0) {
                stats.energyPerSecond / stats.activeGenerators
            } else {
                0.0
            }
            
            // CHANGE DETECTION: Compare current stats with previous snapshot to see if this
            // generator's stats have changed. If so, trigger visual feedback animations.
            val previousStats = previousGeneratorStats[genId]
            val levelChanged = previousStats?.first != avgLevel
            val outputChanged = previousStats?.second != estimatedOutput
            
            // Store current stats for next comparison cycle
            previousGeneratorStats[genId] = Pair(avgLevel, estimatedOutput)
            
            // Bind level text
            val levelTextView = generatorView.findViewById<TextView?>(R.id.text_upgrade_level)
            levelTextView?.text = "Level: ~$avgLevel"
            
            // Bind output text
            val outputTextView = generatorView.findViewById<TextView?>(R.id.text_upgrade_output)
            outputTextView?.text = "~${formatEnergy(estimatedOutput)}/s"
            
            // VISUAL FEEDBACK: If level or output changed, animate to draw user's attention
            if (levelChanged || outputChanged) {
                // 1. FLASH ANIMATION on the text views that changed
                // Scale + pulse to make the change obvious. Users reported not seeing updates—
                // this fixes that by making changes impossible to miss.
                val flashAnim = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.value_change_flash)
                
                if (levelChanged && levelTextView != null) {
                    levelTextView.startAnimation(flashAnim)
                    
                    // 2. +1 LEVEL INDICATOR: Show floating "+1" that fades upward
                    // This mimics RPG damage numbers—instant recognition that level increased.
                    val deltaIndicator = generatorView.findViewById<TextView?>(R.id.text_level_delta)
                    deltaIndicator?.let { delta ->
                        // Calculate level delta to show correct +N value
                        val levelDelta = if (previousStats != null) {
                            avgLevel - previousStats.first
                        } else {
                            1  // First time seeing this generator, assume +1
                        }
                        
                        // Only show if delta is positive (level increased)
                        if (levelDelta > 0) {
                            delta.text = "+$levelDelta"
                            delta.alpha = 1f
                            
                            // Animate: fade up and out over 1 second
                            delta.animate()
                                .alpha(0f)
                                .translationYBy(-30f)  // Float upward
                                .setDuration(1000)
                                .setStartDelay(200)  // Brief delay so user sees it appear first
                                .withEndAction {
                                    // Reset position for next animation
                                    delta.translationY = 0f
                                }
                                .start()
                        }
                    }
                }
                
                if (outputChanged && outputTextView != null) {
                    outputTextView.startAnimation(flashAnim)
                }
                
                // 3. HIGHLIGHT OVERLAY: Green flash on entire row
                // Reinforces that "this generator" is the one that upgraded. Multiple generators
                // might update at once (bulk upgrades), so per-row feedback clarifies which ones.
                val highlightView = generatorView.findViewById<View?>(R.id.view_update_highlight)
                highlightView?.let { highlight ->
                    // Flash green background briefly
                    highlight.alpha = 0.3f
                    highlight.animate()
                        .alpha(0f)
                        .setDuration(800)
                        .setStartDelay(100)
                        .start()
                }
            }
            
            // Upgrade button sends command to Unity and immediately requests fresh stats.
            // The stats observer will fire, triggering syncStatsPanels(), which re-binds this
            // panel with updated numbers. This creates a fast feedback loop (~100ms delay).
            generatorView.findViewById<Button?>(R.id.btn_upgrade)?.apply {
                text = "Tap to Upgrade"
                setOnClickListener {
                    // Visual feedback on button press—scale down slightly to show it's "pressed"
                    // This is instant tactile feedback before Unity processes the upgrade.
                    animate()
                        .scaleX(0.95f)
                        .scaleY(0.95f)
                        .setDuration(50)
                        .withEndAction {
                            // Spring back to normal size
                            animate()
                                .scaleX(1.0f)
                                .scaleY(1.0f)
                                .setDuration(150)
                                .start()
                        }
                        .start()
                    
                    // Send upgrade command to Unity via PowerManager bridge
                    PowerManager.upgradeGenerator(genId)
                    
                    // Request immediate stats update so UI reflects the new level/output.
                    // Without this, user would have to close and reopen the panel to see changes.
                    // The stats observer will fire → syncStatsPanels() → bindBuildingPanel() with
                    // updated values → change detection → visual feedback animations triggered.
                    Stats.requestUnitySnapshot()
                    
                    // Show toast for immediate feedback while we wait for Unity's response.
                    // Toast survives even if the stats update causes a panel rebind.
                    Toast.makeText(this@MainActivity, "Upgrading $genId...", Toast.LENGTH_SHORT).show()
                }
            }
            
            // Entire row is also clickable for burst energy (quick-tap mechanic).
            // This doesn't affect upgrades, just gives instant energy boost.
            generatorView.setOnClickListener {
                // Ripple effect on row tap for immediate feedback
                generatorView.isPressed = true
                generatorView.postDelayed({ generatorView.isPressed = false }, 100)
                
                PowerManager.clickGenerator(genId)
                Stats.requestUnitySnapshot()
            }
            
            container?.addView(generatorView)
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
    private fun bindLeaderboardPanel(panel: View, payload: UnityLeaderboardPayload?) {
        val loading = panel.findViewById<ProgressBar?>(R.id.leaderboard_loading)
        val empty = panel.findViewById<TextView?>(R.id.text_leaderboard_empty)
        val listContainer = panel.findViewById<LinearLayout?>(R.id.leaderboard_list_container)
        val lastUpdated = panel.findViewById<TextView?>(R.id.text_leaderboard_last_updated)

        if (payload == null) {
            // Null payload means we're still waiting on the API—show spinner.
            loading?.isVisible = true
            empty?.isVisible = false
            listContainer?.isVisible = false
            return
        }

        loading?.isVisible = false
        lastUpdated?.text = getString(R.string.leaderboard_last_updated, formatTimestamp(payload.timestampUtc))

        val entries = payload.entries
        if (entries.isEmpty()) {
            // Empty list from backend is valid—show friendly placeholder instead of error.
            empty?.isVisible = true
            listContainer?.isVisible = false
        } else {
            empty?.isVisible = false
            listContainer?.isVisible = true
            listContainer?.let { renderLeaderboardEntries(it, entries) }
        }
    }

    // Hot-swap leaderboard data if the panel is currently visible; otherwise do nothing.
    // This keeps us from wasting cycles updating panels that are hidden behind the modal scrim.
    private fun updateLeaderboardPanel() {
        if (currentPanelLayoutId == R.layout.panel_leaderboard) {
            currentPanelView?.let { bindLeaderboardPanel(it, latestLeaderboard) }
        }
    }

    // Friends panel mirrors the leaderboard's three-state approach: loading, empty, populated.
    // We log state transitions here because friend sync was flaky during beta.
    private fun bindFriendsPanel(panel: View, payload: FriendsPayload?) {
        val loading = panel.findViewById<ProgressBar?>(R.id.friends_loading)
        val empty = panel.findViewById<View?>(R.id.text_friends_empty)
        val listContainer = panel.findViewById<LinearLayout?>(R.id.friends_list_container)
        val lastUpdated = panel.findViewById<TextView?>(R.id.text_friends_last_updated)

        if (payload == null) {
            // Show loading state while fetching—display explicit text for clarity.
            loading?.isVisible = true
            empty?.isVisible = false
            listContainer?.isVisible = false
            lastUpdated?.text = "Loading friends..."
            return
        }

        loading?.isVisible = false
        lastUpdated?.text = getString(R.string.friends_last_updated, formatTimestamp(payload.timestampUtc))

        val entries = payload.friends
        if (entries.isEmpty()) {
            // Empty friends list is common for new users—provide prompt to add friends.
            empty?.isVisible = true
            listContainer?.isVisible = false
            Log.d("MainActivity", "Friends list is empty - showing empty state")
        } else {
            // Populate the list and log count for debugging sync edge cases.
            empty?.isVisible = false
            listContainer?.isVisible = true
            listContainer?.let { renderFriendsEntries(it, entries) }
            Log.d("MainActivity", "Displaying ${entries.size} friends")
        }
    }

    // Same visibility-gating pattern as leaderboard; avoid redundant redraws.
    private fun updateFriendsPanel() {
        if (currentPanelLayoutId == R.layout.panel_friends_leaderboard) {
            currentPanelView?.let { bindFriendsPanel(it, latestFriends) }
        }
    }

    // Settings panel only shows save metadata—lightweight update, but gated behind visibility.
    private fun updateSettingsPanel() {
        if (currentPanelLayoutId == R.layout.panel_settings) {
            currentPanelView?.let { panel ->
                val lastSyncText = panel.findViewById<TextView?>(R.id.text_last_save_sync)
                latestSave?.let { save ->
                    // Timestamp is already in UTC ISO format; formatTimestamp handles local conversion.
                    lastSyncText?.text = "Last synced: ${formatTimestamp(save.timestampUtc)}"
                }
            }
        }
    }

    // Tasks are really just a historical log of events; we repopulate on every update.
    // Early returns keep the logic flat and avoid nested null checks.
    private fun updateTasksPanel() {
        if (currentPanelLayoutId != R.layout.panel_tasks_board) return
        val panel = currentPanelView ?: return
        val empty = panel.findViewById<TextView?>(R.id.text_tasks_empty)
        val container = panel.findViewById<LinearLayout?>(R.id.tasks_list_container)
        val history = eventHistory

        if (container == null) return

        if (history.isEmpty()) {
            // No events yet—show placeholder and clear any stale rows.
            empty?.isVisible = true
            container.isVisible = false
            container.removeAllViews()
        } else {
            empty?.isVisible = false
            container.isVisible = true
            renderEventHistory(container, history)
        }
    }

    // Leaderboard rows get badges based on rank. Top 3 get gold/silver/bronze stars.
    // We also inject emoji tags next to names to make high scores visually pop.
    private fun renderLeaderboardEntries(
        container: LinearLayout,
        entries: List<UnityLeaderboardPayload.Entry>
    ) {
        container.removeAllViews()
        entries.forEachIndexed { index, entry ->
            val row = LayoutInflater.from(this).inflate(R.layout.view_leaderboard_row, container, false)
            row.findViewById<TextView?>(R.id.text_rank)?.text = "#${index + 1}"
            
            // Cosmetic tags add personality without cluttering the database schema.
            // Thresholds tuned during user testing—1M is "elite", 100K is "veteran".
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
            row.findViewById<TextView?>(R.id.text_entry_detail)?.text = formatEnergy(entry.score)
            val icon = row.findViewById<ImageView?>(R.id.image_entry_status)
            // Android's built-in star drawables give us a quick rank visual.
            val iconRes = when (index) {
                0 -> android.R.drawable.star_big_on
                in 1..2 -> android.R.drawable.btn_star_big_on
                else -> android.R.drawable.btn_star_big_off
            }
            icon?.setImageResource(iconRes)
            container.addView(row)
        }
    }

    // Friends list reuses the leaderboard row layout but swaps rank for contribution rate.
    // Online presence indicators let users see who's active without opening a separate screen.
    private fun renderFriendsEntries(
        container: LinearLayout,
        entries: List<FriendProfile>
    ) {
        container.removeAllViews()
        entries.forEachIndexed { index, friend ->
            val row = LayoutInflater.from(this).inflate(R.layout.view_leaderboard_row, container, false)
            row.findViewById<TextView?>(R.id.text_rank)?.text = "#${index + 1}"
            
            // Contribution-based tags mirror leaderboard logic but use hourly rate thresholds.
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
            // Detail line packs contribution and last-seen timestamp into a compact format.
            row.findViewById<TextView?>(R.id.text_entry_detail)?.text =
                "${formatEnergy(friend.contributionPerHour)} /hr • ${formatTimestamp(friend.lastInteractionUtc)}"
            val icon = row.findViewById<ImageView?>(R.id.image_entry_status)
            // Presence icons are Android system drawables—green dot vs. gray ghost.
            icon?.setImageResource(
                if (friend.isOnline) android.R.drawable.presence_online else android.R.drawable.presence_invisible
            )
            container.addView(row)
        }
    }

    // Event history could be huge—cap at 10 to avoid scroll lag. Users rarely check past that.
    // Each row shows whether the event is currently active or just historical context.
    private fun renderEventHistory(container: LinearLayout, history: List<UnityEventPayload>) {
        container.removeAllViews()
        history.take(10).forEach { event ->
            val row = LayoutInflater.from(this).inflate(R.layout.view_task_item, container, false)
            // Active events get a special prefix to draw attention; past events stay neutral.
            row.findViewById<TextView?>(R.id.text_task_title)?.text = when {
                event.isStarting && currentActiveEvent?.id == event.id ->
                    getString(R.string.tasks_event_active, event.title)
                event.isStarting ->
                    getString(R.string.tasks_event_active, event.title)
                else ->
                    getString(R.string.tasks_event_inactive, event.title)
            }

            // Detail line compresses description, multiplier, and duration into one scannable string.
            // Bullet separators keep it readable without adding extra views.
            val detail = buildString {
                append(event.description)
                append(" • ")
                append(getString(R.string.tasks_event_multiplier, event.productionMultiplier))
                append(" • ")
                append(getString(R.string.tasks_event_duration, formatDuration(event.durationSeconds.toDouble())))
            }
            row.findViewById<TextView?>(R.id.text_task_detail)?.text = detail
            row.findViewById<TextView?>(R.id.text_task_badge)?.text = formatTimestamp(event.timestampUtc)
            val icon = row.findViewById<ImageView?>(R.id.image_task_state)
            // Checkbox icons are a quick visual cue: checked = active, unchecked = past.
            icon?.setImageResource(
                if (event.isStarting) android.R.drawable.checkbox_on_background else android.R.drawable.checkbox_off_background
            )
            container.addView(row)
        }
    }

    // Energy values get huge (millions), so we use thousand separators everywhere.
    // Pre-compiled DecimalFormat prevents repeated pattern parsing.
    private fun formatEnergy(value: Double): String = numberFormatter.format(value)

    // Duration formatter progressively scales units: seconds → minutes → hours → days.
    // We only show the two most significant units to keep strings short.
    private fun formatDuration(seconds: Double): String {
        if (seconds <= 0.0) return "0s"
        var remaining = seconds.toLong()
        val hours = remaining / 3600
        remaining %= 3600
        val minutes = remaining / 60
        val secs = remaining % 60
        // Parts array accumulates non-zero units; we join them with spaces at the end.
        // This approach avoids string concatenation in a loop (minor perf win).
        val parts = mutableListOf<String>()
        if (hours > 0) parts.add("${hours}h")
        // Show minutes if they exist OR if we already have hours (e.g., "2h 0m" is clearer than "2h").
        if (minutes > 0 || hours > 0) parts.add("${minutes}m")
        // Always show seconds if nothing else exists; otherwise only if non-zero.
        if (secs > 0 || parts.isEmpty()) parts.add("${secs}s")
        return parts.joinToString(" ")
    }

    // Timestamps arrive from backend as UTC ISO strings; we parse and convert to local time.
    // Defensive coding: null/blank strings and parse errors both fall back to "Unknown".
    // This prevents crashes if the backend sends malformed data during rolling updates.
    private fun formatTimestamp(isoString: String?): String {
        if (isoString.isNullOrBlank()) return getString(R.string.timestamp_unknown)
        return try {
            val instant = Instant.parse(isoString)
            // timestampFormatter was initialized with system default zone at class load time.
            timestampFormatter.format(instant)
        } catch (ex: DateTimeParseException) {
            // Log would be useful here, but we're avoiding log spam from bad legacy data.
            getString(R.string.timestamp_unknown)
        }
    }

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
    
    /**
     * Applies the saved locale preference on activity startup.
     * MUST be called in onCreate() BEFORE view inflation, or we get a jarring language
     * flash where views briefly show English before switching to saved language.
     * 
     * This method is defensive—if saved index is out of bounds (e.g., we removed a language),
     * we fall back to English instead of crashing.
     */
    private fun applySavedLocale() {
        val savedIndex = settingsPrefs.getInt(KEY_LANGUAGE_INDEX, DEFAULT_LANGUAGE_INDEX)
        
        // Validate array bounds to prevent IndexOutOfBoundsException
        val languageCount = resources.getStringArray(R.array.settings_languages).size
        if (savedIndex in 0 until languageCount) {
            applyLocale(savedIndex)
        } else {
            // Corrupted preference or removed language—reset to default
            Log.w("MainActivity", "Invalid language index $savedIndex, resetting to default")
            settingsPrefs.edit().putInt(KEY_LANGUAGE_INDEX, DEFAULT_LANGUAGE_INDEX).apply()
        }
    }
    
    /**
     * Updates the app's locale configuration based on the given language index.
     * This method handles the messy reality of Android's evolving locale APIs:
     * 
     * - API 17-23: Use Configuration.locale (deprecated but required for old devices)
     * - API 24+: Use Configuration.setLocale() + LocaleList (modern approach)
     * - API 33+: Should use per-app language APIs, but we stick with Configuration
     *   for consistency since we target minSdk 24
     * 
     * The key insight: updateConfiguration() doesn't fully refresh the activity—you MUST
     * call recreate() afterward to see changes in all views. We learned this the hard way
     * after spending 3 hours debugging "half-translated" screens.
     * 
     * @param languageIndex Index into the settings_languages and language_codes arrays
     */
    private fun applyLocale(languageIndex: Int) {
        try {
            val localeCodes = resources.getStringArray(R.array.language_codes)
            
            // Bounds check again (paranoid but prevents crashes in production)
            if (languageIndex !in localeCodes.indices) {
                Log.e("MainActivity", "Language index $languageIndex out of bounds")
                return
            }
            
            val localeCode = localeCodes[languageIndex]
            
            // Empty string means default locale (English)
            val locale = if (localeCode.isEmpty()) {
                Locale.getDefault()
            } else {
                Locale(localeCode)
            }
            
            // Update default locale first—this affects NumberFormat, DateFormat, etc.
            Locale.setDefault(locale)
            
            // Now update the app's configuration. This is where Android version differences matter.
            val config = Configuration(resources.configuration)
            
            // For API 24+, we should use setLocale() and setLocales(), but Configuration.locale
            // still works and is simpler. We suppress the deprecation warning because supporting
            // the modern API would require us to duplicate logic for older devices anyway.
            @Suppress("DEPRECATION")
            config.locale = locale
            
            // Create a new context with updated config. This is critical—just calling
            // updateConfiguration() on the existing context doesn't fully propagate changes.
            val updatedContext = createConfigurationContext(config)
            
            // Also update the base context configuration for good measure. Not 100% sure this
            // is necessary after createConfigurationContext(), but it doesn't hurt and we had
            // edge cases where strings in toasts stayed in the old language without this.
            @Suppress("DEPRECATION")
            resources.updateConfiguration(config, resources.displayMetrics)
            
            Log.d("MainActivity", "Applied locale: ${locale.language} (index $languageIndex)")
            
        } catch (e: Exception) {
            // Catch-all to prevent locale changes from crashing the app. In the worst case,
            // we stay in the current language rather than force-closing.
            Log.e("MainActivity", "Failed to apply locale: ${e.message}", e)
        }
    }
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
