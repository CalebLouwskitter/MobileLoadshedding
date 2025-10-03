package com.prog7314.IdleLoadsheddingMobile

import android.content.ClipData //Android, 2025.
import android.content.ClipboardManager //Android, 2025.
import android.content.Context //Android, 2025.
import android.os.Bundle //Android, 2025.
import android.util.Log //Android, 2025.
import android.view.LayoutInflater //Android, 2025.
import android.view.View //Android, 2025.
import android.widget.Button //Android, 2025.
import android.widget.EditText //Android, 2025.
import android.widget.FrameLayout //Android, 2025.
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
import java.time.format.DateTimeFormatter //Oracle, 2025.
import java.time.format.DateTimeParseException //Oracle, 2025.
import kotlin.math.roundToInt //Kotlin, 2025.

class MainActivity : UnityPlayerGameActivity() { //Unity Technologies, 2025.

    private lateinit var modalContainer: FrameLayout
    private lateinit var modalScrim: View

    private lateinit var textEnergyAmount: TextView
    private lateinit var textEnergyPerSecond: TextView
    private lateinit var textActiveGenerators: TextView
    private lateinit var textEventStatus: TextView
    private lateinit var textSafeIdleHours: TextView

    private var currentPanelLayoutId: Int? = null
    private var currentPanelView: View? = null

    private var latestStats: UnityStatsSnapshot? = null
    private var latestLeaderboard: UnityLeaderboardPayload? = null
    private var latestFriends: FriendsPayload? = null
    private var latestSave: UnitySavePayload? = null
    private var currentActiveEvent: UnityEventPayload? = null
    private var eventHistory: List<UnityEventPayload> = emptyList()

    private val numberFormatter = DecimalFormat("#,##0.##")
    private val timestampFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
        .withZone(ZoneId.systemDefault())
    private val gson = Gson()
    private val auth = FirebaseAuth.getInstance()

    private val statsObserver = object : Stats.Observer {
        override fun onStatsUpdated(snapshot: UnityStatsSnapshot) {
            runOnUiThread {
                latestStats = snapshot
                updateHud(snapshot)
                syncStatsPanels(snapshot)
            }
        }
    }

    private val leaderboardObserver = object : Leaderboard.Observer {
        override fun onLeaderboardUpdated(payload: UnityLeaderboardPayload) {
            Log.d("MainActivity", "Leaderboard observer notified with ${payload.entries.size} entries")
            runOnUiThread {
                latestLeaderboard = payload
                updateLeaderboardPanel()
            }
        }
    }

    private val friendsObserver = object : FriendsList.Observer {
        override fun onFriendsUpdated(payload: FriendsPayload) {
            runOnUiThread {
                latestFriends = payload
                updateFriendsPanel()
            }
        }
    }

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
            // Quick tap for burst energy from active generators
            PowerManager.clickGenerator("generator_1")
            Stats.requestUnitySnapshot()
        }
        
        overlay.findViewById<Button>(R.id.btn_nav_power).setOnLongClickListener {
            showPanel(R.layout.panel_electricity_upgrade)
            true
        }

        overlay.findViewById<Button>(R.id.btn_nav_tasks).setOnClickListener {
            showPanel(R.layout.panel_tasks_board)
        }

        addContentView(overlay, layoutParams)

        SaveSync.initialize(this)

        window.decorView.post {
            UnityBridge.hideUnityHud()
            Stats.requestUnitySnapshot()
        }

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
        Stats.removeObserver(statsObserver)
        Leaderboard.removeObserver(leaderboardObserver)
        FriendsList.removeObserver(friendsObserver)
        Events.removeObserver(eventsObserver)
        SaveSync.removeListener(saveListener)
        super.onDestroy()
    }

    private fun showPanel(layoutResId: Int) {
        val panelView = LayoutInflater.from(this).inflate(layoutResId, modalContainer, false)
        modalContainer.removeAllViews()
        modalContainer.addView(panelView)
        modalContainer.visibility = View.VISIBLE
        modalScrim.visibility = View.VISIBLE

        currentPanelLayoutId = layoutResId
        currentPanelView = panelView

        panelView.findViewById<View?>(R.id.close_button)?.setOnClickListener { hidePanel() }

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
        modalContainer.removeAllViews()
        modalContainer.visibility = View.GONE
        modalScrim.visibility = View.GONE
        currentPanelLayoutId = null
        currentPanelView = null
    }

    private fun configureElectricityPanel(panel: View) {
        panel.findViewById<View?>(R.id.tab_building)?.setOnClickListener {
            showPanel(R.layout.panel_building_upgrade)
        }

        panel.findViewById<Button?>(R.id.btn_refresh_production)?.setOnClickListener {
            Stats.requestUnitySnapshot()
            SaveSync.requestUnitySave()
        }

        bindElectricityPanel(panel, latestStats, latestSave, currentActiveEvent)
    }

    private fun configureBuildingPanel(panel: View) {
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
            showPanel(R.layout.panel_friends_leaderboard)
        }

        panel.findViewById<View?>(R.id.btn_profile_logout)?.setOnClickListener {
            Toast.makeText(this, getString(R.string.toast_logout_placeholder), Toast.LENGTH_SHORT).show()
            hidePanel()
        }

        panel.findViewById<View?>(R.id.btn_profile_details)?.setOnClickListener {
            Toast.makeText(this, "Profile details placeholder", Toast.LENGTH_SHORT).show()
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
        panel.findViewById<Button?>(R.id.btn_cancel_profile)?.setOnClickListener {
            showPanel(R.layout.panel_profile)
        }

        panel.findViewById<Button?>(R.id.btn_save_profile)?.setOnClickListener {
            Toast.makeText(this, "Profile saved (placeholder)", Toast.LENGTH_SHORT).show()
            showPanel(R.layout.panel_profile)
        }
    }

    private fun configureSettingsPanel(panel: View) {
        // Upload save button
        panel.findViewById<Button?>(R.id.btn_upload_save)?.setOnClickListener {
            SaveSync.requestUnitySave()
            Toast.makeText(this, getString(R.string.toast_save_uploaded), Toast.LENGTH_SHORT).show()
        }

        // Sync save button
        panel.findViewById<Button?>(R.id.btn_sync_save)?.setOnClickListener {
            Stats.requestUnitySnapshot()
            SaveSync.requestUnitySave()
            Toast.makeText(this, getString(R.string.toast_save_synced), Toast.LENGTH_SHORT).show()
        }

        // Display last sync time
        val lastSyncText = panel.findViewById<TextView?>(R.id.text_last_save_sync)
        latestSave?.let { save ->
            lastSyncText?.text = "Last synced: ${formatTimestamp(save.timestampUtc)}"
        }
    }

    private fun configureLeaderboardPanel(panel: View) {
        Log.d("MainActivity", "Configuring leaderboard panel")
        
        panel.findViewById<View?>(R.id.btn_tab_friends)?.setOnClickListener {
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
        
        panel.findViewById<Button?>(R.id.btn_show_global)?.setOnClickListener {
            showPanel(R.layout.panel_leaderboard)
        }

        panel.findViewById<Button?>(R.id.btn_show_friends)?.setOnClickListener {
            Log.d("MainActivity", "Refreshing friends list from backend")
            FriendsList.fetchBackendFriends { result ->
                result.onSuccess { payload ->
                    Log.d("MainActivity", "SUCCESS: Friends fetched with ${payload.friends.size} entries")
                }.onFailure { error ->
                    Log.e("MainActivity", "FAILED: Friends fetch error: ${error.message}", error)
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
        updateTasksPanel()
    }

    private fun updateHud(snapshot: UnityStatsSnapshot) {
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

    private fun bindBuildingPanel(panel: View, stats: UnityStatsSnapshot?) {
        val container = panel.findViewById<LinearLayout?>(R.id.list_building_upgrades)
        container?.removeAllViews()

        if (stats == null) {
            val emptyText = TextView(this)
            emptyText.text = "Loading generator data..."
            emptyText.setTextColor(0xFF052E45.toInt())
            emptyText.textSize = 14f
            emptyText.setPadding(16, 16, 16, 16)
            container?.addView(emptyText)
            return
        }

        // Create upgrade rows for each potential generator (up to 4 generators in the scene)
        val generatorIds = listOf("generator_1", "generator_2", "generator_3", "generator_4")
        val avgLevel = if (stats.activeGenerators > 0) stats.generatorLevelSum / stats.activeGenerators else 1
        
        generatorIds.forEachIndexed { index, genId ->
            val generatorView = LayoutInflater.from(this).inflate(R.layout.view_upgrade_item, container, false)
            generatorView.findViewById<TextView?>(R.id.text_upgrade_name)?.text = "Generator ${index + 1}"
            generatorView.findViewById<TextView?>(R.id.text_upgrade_level)?.text = "Level: ~$avgLevel"
            
            val estimatedOutput = (stats.energyPerSecond / stats.activeGenerators.coerceAtLeast(1)).coerceAtLeast(0.0)
            generatorView.findViewById<TextView?>(R.id.text_upgrade_output)?.text = "~${formatEnergy(estimatedOutput)}/s"
            
            generatorView.findViewById<Button?>(R.id.btn_upgrade)?.apply {
                text = "Tap to Upgrade"
                setOnClickListener {
                    PowerManager.upgradeGenerator(genId)
                    Stats.requestUnitySnapshot()
                    Toast.makeText(this@MainActivity, "Upgrading $genId", Toast.LENGTH_SHORT).show()
                }
            }
            
            // Add click listener to entire row for burst energy
            generatorView.setOnClickListener {
                PowerManager.clickGenerator(genId)
                Stats.requestUnitySnapshot()
            }
            
            container?.addView(generatorView)
        }
    }

    private fun renderDistrictsPanel(panel: View, stats: UnityStatsSnapshot?) {
        val container = panel.findViewById<LinearLayout?>(R.id.list_districts) ?: return
        container.removeAllViews()

        val districts = stats?.districts.orEmpty()
        if (districts.isEmpty()) {
            val padding = (panel.resources.displayMetrics.density * 12).toInt()
            val emptyView = TextView(panel.context).apply {
                setText(R.string.districts_empty)
                textAlignment = View.TEXT_ALIGNMENT_CENTER
                setPadding(0, padding, 0, padding)
            }
            container.addView(emptyView)
            return
        }

        val inflater = LayoutInflater.from(panel.context)
        districts.forEach { district ->
            val row = inflater.inflate(R.layout.item_district_status, container, false)
            row.findViewById<TextView?>(R.id.text_district_name)?.text = district.name
            row.findViewById<TextView?>(R.id.text_district_level)?.text =
                getString(R.string.district_level_format, district.level)

            val powerText = getString(
                R.string.district_output_format,
                formatEnergy(district.powerPerSecond),
                formatEnergy(district.moneyPerSecond)
            )
            row.findViewById<TextView?>(R.id.text_district_output)?.text = powerText

            val efficiencyPercent = (district.efficiency * 100).roundToInt().coerceIn(0, 100)
            row.findViewById<TextView?>(R.id.text_district_efficiency)?.text =
                getString(R.string.district_efficiency_format, efficiencyPercent)

            val idleSeconds = (district.hoursUntilMaintenance * 3600).coerceAtLeast(0.0)
            val idleText = if (idleSeconds <= 0.0) {
                getString(R.string.timestamp_unknown)
            } else {
                formatDuration(idleSeconds)
            }
            row.findViewById<TextView?>(R.id.text_district_idle_time)?.text =
                getString(R.string.district_idle_time_format, idleText)

            row.findViewById<Button?>(R.id.btn_district_service)?.setOnClickListener {
                UnityBridge.sendMessageToUnity("DistrictManager", "ServiceDistrict", district.id)
                Stats.requestUnitySnapshot()
            }

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
                    UnityBridge.sendMessageToUnity("DistrictManager", "UpgradeDistrict", district.id)
                    Stats.requestUnitySnapshot()
                }
            }

            container.addView(row)
        }
    }

    private fun bindLeaderboardPanel(panel: View, payload: UnityLeaderboardPayload?) {
        val loading = panel.findViewById<ProgressBar?>(R.id.leaderboard_loading)
        val empty = panel.findViewById<TextView?>(R.id.text_leaderboard_empty)
        val listContainer = panel.findViewById<LinearLayout?>(R.id.leaderboard_list_container)
        val lastUpdated = panel.findViewById<TextView?>(R.id.text_leaderboard_last_updated)

        if (payload == null) {
            loading?.isVisible = true
            empty?.isVisible = false
            listContainer?.isVisible = false
            return
        }

        loading?.isVisible = false
        lastUpdated?.text = getString(R.string.leaderboard_last_updated, formatTimestamp(payload.timestampUtc))

        val entries = payload.entries
        if (entries.isEmpty()) {
            empty?.isVisible = true
            listContainer?.isVisible = false
        } else {
            empty?.isVisible = false
            listContainer?.isVisible = true
            listContainer?.let { renderLeaderboardEntries(it, entries) }
        }
    }

    private fun updateLeaderboardPanel() {
        if (currentPanelLayoutId == R.layout.panel_leaderboard) {
            currentPanelView?.let { bindLeaderboardPanel(it, latestLeaderboard) }
        }
    }

    private fun bindFriendsPanel(panel: View, payload: FriendsPayload?) {
        val loading = panel.findViewById<ProgressBar?>(R.id.friends_loading)
        val empty = panel.findViewById<View?>(R.id.text_friends_empty)
        val listContainer = panel.findViewById<LinearLayout?>(R.id.friends_list_container)
        val lastUpdated = panel.findViewById<TextView?>(R.id.text_friends_last_updated)

        if (payload == null) {
            // Show loading state while fetching
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
            // Show empty state with helpful message
            empty?.isVisible = true
            listContainer?.isVisible = false
            Log.d("MainActivity", "Friends list is empty - showing empty state")
        } else {
            // Show friends list
            empty?.isVisible = false
            listContainer?.isVisible = true
            listContainer?.let { renderFriendsEntries(it, entries) }
            Log.d("MainActivity", "Displaying ${entries.size} friends")
        }
    }

    private fun updateFriendsPanel() {
        if (currentPanelLayoutId == R.layout.panel_friends_leaderboard) {
            currentPanelView?.let { bindFriendsPanel(it, latestFriends) }
        }
    }

    private fun updateSettingsPanel() {
        if (currentPanelLayoutId == R.layout.panel_settings) {
            currentPanelView?.let { panel ->
                val lastSyncText = panel.findViewById<TextView?>(R.id.text_last_save_sync)
                latestSave?.let { save ->
                    lastSyncText?.text = "Last synced: ${formatTimestamp(save.timestampUtc)}"
                }
            }
        }
    }

    private fun updateTasksPanel() {
        if (currentPanelLayoutId != R.layout.panel_tasks_board) return
        val panel = currentPanelView ?: return
        val empty = panel.findViewById<TextView?>(R.id.text_tasks_empty)
        val container = panel.findViewById<LinearLayout?>(R.id.tasks_list_container)
        val history = eventHistory

        if (container == null) return

        if (history.isEmpty()) {
            empty?.isVisible = true
            container.isVisible = false
            container.removeAllViews()
        } else {
            empty?.isVisible = false
            container.isVisible = true
            renderEventHistory(container, history)
        }
    }

    private fun renderLeaderboardEntries(
        container: LinearLayout,
        entries: List<UnityLeaderboardPayload.Entry>
    ) {
        container.removeAllViews()
        entries.forEachIndexed { index, entry ->
            val row = LayoutInflater.from(this).inflate(R.layout.view_leaderboard_row, container, false)
            row.findViewById<TextView?>(R.id.text_rank)?.text = "#${index + 1}"
            
            // Add cosmetic tag to display name based on score
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
            val iconRes = when (index) {
                0 -> android.R.drawable.star_big_on
                in 1..2 -> android.R.drawable.btn_star_big_on
                else -> android.R.drawable.btn_star_big_off
            }
            icon?.setImageResource(iconRes)
            container.addView(row)
        }
    }

    private fun renderFriendsEntries(
        container: LinearLayout,
        entries: List<FriendProfile>
    ) {
        container.removeAllViews()
        entries.forEachIndexed { index, friend ->
            val row = LayoutInflater.from(this).inflate(R.layout.view_leaderboard_row, container, false)
            row.findViewById<TextView?>(R.id.text_rank)?.text = "#${index + 1}"
            
            // Add cosmetic tag to display name based on contribution
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
                "${formatEnergy(friend.contributionPerHour)} /hr • ${formatTimestamp(friend.lastInteractionUtc)}"
            val icon = row.findViewById<ImageView?>(R.id.image_entry_status)
            icon?.setImageResource(
                if (friend.isOnline) android.R.drawable.presence_online else android.R.drawable.presence_invisible
            )
            container.addView(row)
        }
    }

    private fun renderEventHistory(container: LinearLayout, history: List<UnityEventPayload>) {
        container.removeAllViews()
        history.take(10).forEach { event ->
            val row = LayoutInflater.from(this).inflate(R.layout.view_task_item, container, false)
            row.findViewById<TextView?>(R.id.text_task_title)?.text = when {
                event.isStarting && currentActiveEvent?.id == event.id ->
                    getString(R.string.tasks_event_active, event.title)
                event.isStarting ->
                    getString(R.string.tasks_event_active, event.title)
                else ->
                    getString(R.string.tasks_event_inactive, event.title)
            }

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
            icon?.setImageResource(
                if (event.isStarting) android.R.drawable.checkbox_on_background else android.R.drawable.checkbox_off_background
            )
            container.addView(row)
        }
    }

    private fun formatEnergy(value: Double): String = numberFormatter.format(value)

    private fun formatDuration(seconds: Double): String {
        if (seconds <= 0.0) return "0s"
        var remaining = seconds.toLong()
        val hours = remaining / 3600
        remaining %= 3600
        val minutes = remaining / 60
        val secs = remaining % 60
        val parts = mutableListOf<String>()
        if (hours > 0) parts.add("${hours}h")
        if (minutes > 0 || hours > 0) parts.add("${minutes}m")
        if (secs > 0 || parts.isEmpty()) parts.add("${secs}s")
        return parts.joinToString(" ")
    }

    private fun formatTimestamp(isoString: String?): String {
        if (isoString.isNullOrBlank()) return getString(R.string.timestamp_unknown)
        return try {
            val instant = Instant.parse(isoString)
            timestampFormatter.format(instant)
        } catch (ex: DateTimeParseException) {
            getString(R.string.timestamp_unknown)
        }
    }

    private fun formatEventDescription(event: UnityEventPayload): String {
        val state = if (event.isStarting) getString(R.string.tasks_event_active, event.title) else getString(
            R.string.tasks_event_inactive,
            event.title
        )
        val multiplier = getString(R.string.tasks_event_multiplier, event.productionMultiplier)
        return "$state • $multiplier"
    }

    private fun sendDistrictAction(methodName: String, districtId: String) {
        if (districtId.isBlank()) return
        val payload = gson.toJson(DistrictAction(districtId)) //Google, 2025.
        UnityBridge.sendJsonToUnity("UnityMessageRouter", methodName, payload)
    }

    private data class DistrictAction(val districtId: String)
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
