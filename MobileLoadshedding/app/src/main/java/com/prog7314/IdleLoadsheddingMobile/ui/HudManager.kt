package com.prog7314.IdleLoadsheddingMobile.ui

import android.content.Context
import android.widget.TextView
import androidx.core.view.isVisible
import com.prog7314.IdleLoadsheddingMobile.R
import com.prog7314.IdleLoadsheddingMobile.data.models.UnityEventPayload
import com.prog7314.IdleLoadsheddingMobile.data.models.UnityStatsSnapshot
import com.prog7314.IdleLoadsheddingMobile.utils.FormatUtils

/**
 * Manages HUD (Heads-Up Display) updates.
 * Extracted from MainActivity to separate concerns.
 * 
 * The HUD shows:
 * - Current energy/money
 * - Energy per second
 * - Active generators count
 * - Event status banner
 * - Safe idle time
 */
class HudManager(
    private val context: Context,
    private val formatUtils: FormatUtils,
    private val textEnergyAmount: TextView,
    private val textEnergyPerSecond: TextView,
    private val textActiveGenerators: TextView,
    private val textEventStatus: TextView,
    private val textSafeIdleHours: TextView
) {
    
    /**
     * Updates HUD with latest stats snapshot.
     */
    fun updateHud(snapshot: UnityStatsSnapshot, currentActiveEvent: UnityEventPayload?) {
        // Credits prefer money, then energy, then lifetime generated output
        val credits = when {
            snapshot.currentMoney > 0.0 -> snapshot.currentMoney
            snapshot.currentEnergy > 0.0 -> snapshot.currentEnergy
            else -> snapshot.totalEnergyGenerated
        }
        textEnergyAmount.text = formatUtils.formatEnergy(credits)
        
        textEnergyPerSecond.text = context.getString(
            R.string.hud_energy_per_second_format,
            formatUtils.formatEnergy(snapshot.energyPerSecond)
        )
        
        textActiveGenerators.text = context.getString(
            R.string.hud_generators_format,
            snapshot.activeGenerators,
            snapshot.generatorLevelSum
        )
        
        val safeSeconds = when {
            snapshot.hoursUntilMaintenance > 0.0 -> snapshot.hoursUntilMaintenance * 3600.0
            snapshot.offlineSeconds > 0.0 -> snapshot.offlineSeconds
            else -> 0.0
        }
        val safeIdle = if (safeSeconds > 0.0) {
            formatUtils.formatDuration(safeSeconds)
        } else {
            context.getString(R.string.timestamp_unknown)
        }
        textSafeIdleHours.text = context.getString(R.string.hud_safe_idle_format, safeIdle)
        
        // Update event banner
        updateEventBanner(currentActiveEvent)
    }
    
    /**
     * Updates the event status banner.
     */
    fun updateEventBanner(event: UnityEventPayload?) {
        if (event != null) {
            textEventStatus.isVisible = true
            textEventStatus.text = context.getString(
                R.string.hud_event_active,
                event.title,
                event.productionMultiplier
            )
        } else {
            textEventStatus.isVisible = false
            textEventStatus.text = context.getString(R.string.hud_event_inactive)
        }
    }
}
