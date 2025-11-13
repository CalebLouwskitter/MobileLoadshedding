package com.prog7314.IdleLoadsheddingMobile.ui.panels

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.prog7314.IdleLoadsheddingMobile.R
import com.prog7314.IdleLoadsheddingMobile.data.models.UnityEventPayload
import com.prog7314.IdleLoadsheddingMobile.data.models.UnitySavePayload
import com.prog7314.IdleLoadsheddingMobile.data.models.UnityStatsSnapshot
import com.prog7314.IdleLoadsheddingMobile.unity.SaveSync
import com.prog7314.IdleLoadsheddingMobile.unity.Stats
import com.prog7314.IdleLoadsheddingMobile.utils.FormatUtils
import kotlin.math.roundToInt

/**
 * Handles electricity/production panel configuration and data binding.
 * Extracted from MainActivity to separate concerns.
 */
class ElectricityPanelBinder(
    private val context: Context,
    private val formatUtils: FormatUtils,
    private val districtsPanelBinder: DistrictsPanelBinder
) {
    
    /**
     * Configures electricity panel UI elements and event handlers.
     */
    fun configure(
        panel: View,
        stats: UnityStatsSnapshot?,
        save: UnitySavePayload?,
        event: UnityEventPayload?,
        eventHistory: List<UnityEventPayload>,
        onShowPanel: (Int) -> Unit
    ) {
        // Tab navigation
        panel.findViewById<View?>(R.id.tab_building)?.setOnClickListener {
            onShowPanel(R.layout.panel_building_upgrade)
        }
        
        // Refresh button
        panel.findViewById<Button?>(R.id.btn_refresh_production)?.setOnClickListener {
            Stats.requestUnitySnapshot()
            SaveSync.requestUnitySave()
        }
        
        // Bind data
        bindData(panel, stats, save, event, eventHistory)
    }
    
    /**
     * Binds electricity/production data to UI elements.
     */
    fun bindData(
        panel: View,
        stats: UnityStatsSnapshot?,
        save: UnitySavePayload?,
        event: UnityEventPayload?,
        eventHistory: List<UnityEventPayload>
    ) {
        // Format all the stat strings
        val currentEnergy = stats?.let { 
            context.getString(R.string.electricity_current_energy, formatUtils.formatEnergy(it.currentEnergy))
        } ?: context.getString(R.string.electricity_current_energy, context.getString(R.string.timestamp_unknown))
        
        val production = stats?.let {
            context.getString(R.string.electricity_production, formatUtils.formatEnergy(it.energyPerSecond))
        } ?: context.getString(R.string.electricity_production, context.getString(R.string.timestamp_unknown))
        
        val generators = stats?.let {
            context.getString(R.string.electricity_generators, it.activeGenerators, it.generatorLevelSum)
        } ?: context.getString(R.string.electricity_generators, 0, 0)
        
        val currency = stats?.let {
            context.getString(R.string.electricity_currency, formatUtils.formatEnergy(it.currentMoney))
        } ?: context.getString(R.string.electricity_currency, context.getString(R.string.timestamp_unknown))
        
        val offline = stats?.let {
            context.getString(R.string.electricity_offline, formatUtils.formatDuration(it.offlineSeconds))
        } ?: context.getString(R.string.electricity_offline, context.getString(R.string.timestamp_unknown))
        
        val safeIdle = stats?.let {
            val seconds = (it.hoursUntilMaintenance * 3600).coerceAtLeast(0.0)
            context.getString(
                R.string.electricity_safe_idle,
                if (seconds <= 0.0) context.getString(R.string.timestamp_unknown) else formatUtils.formatDuration(seconds)
            )
        } ?: context.getString(R.string.electricity_safe_idle, context.getString(R.string.timestamp_unknown))
        
        val coreCharge = stats?.let {
            val percent = (it.coreChargePercent * 100).roundToInt().coerceIn(0, 100)
            context.getString(R.string.electricity_core_charge, percent)
        } ?: context.getString(R.string.electricity_core_charge, 0)
        
        val lastEvent = eventHistory.firstOrNull()?.let {
            context.getString(R.string.electricity_last_event, formatEventDescription(it))
        } ?: context.getString(R.string.electricity_last_event, context.getString(R.string.hud_event_inactive))
        
        val lastSave = save?.let {
            context.getString(R.string.electricity_last_save, formatUtils.formatTimestamp(it.timestampUtc))
        } ?: context.getString(R.string.electricity_last_save, context.getString(R.string.timestamp_unknown))
        
        // Bind all values to UI
        panel.findViewById<TextView?>(R.id.text_current_energy_value)?.text = currentEnergy
        panel.findViewById<TextView?>(R.id.text_current_production_value)?.text = production
        panel.findViewById<TextView?>(R.id.text_generators_value)?.text = generators
        panel.findViewById<TextView?>(R.id.text_currency_value)?.text = currency
        panel.findViewById<TextView?>(R.id.text_offline_time_value)?.text = offline
        panel.findViewById<TextView?>(R.id.text_safe_idle_value)?.text = safeIdle
        panel.findViewById<TextView?>(R.id.text_core_charge_value)?.text = coreCharge
        panel.findViewById<TextView?>(R.id.text_last_event_value)?.text = lastEvent
        panel.findViewById<TextView?>(R.id.text_last_save_value)?.text = lastSave
        
        // Render districts section
        districtsPanelBinder.bindData(panel, stats)
    }
    
    /**
     * Formats event description for display.
     */
    private fun formatEventDescription(event: UnityEventPayload): String {
        val state = if (event.isStarting) {
            context.getString(R.string.tasks_event_active, event.title)
        } else {
            context.getString(R.string.tasks_event_inactive, event.title)
        }
        val multiplier = context.getString(R.string.tasks_event_multiplier, event.productionMultiplier)
        return "$state • $multiplier"
    }
}
