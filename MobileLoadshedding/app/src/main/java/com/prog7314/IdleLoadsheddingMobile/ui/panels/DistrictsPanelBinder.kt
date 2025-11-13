package com.prog7314.IdleLoadsheddingMobile.ui.panels

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.prog7314.IdleLoadsheddingMobile.R
import com.prog7314.IdleLoadsheddingMobile.data.models.UnityStatsSnapshot
import com.prog7314.IdleLoadsheddingMobile.unity.PowerManager
import com.prog7314.IdleLoadsheddingMobile.unity.Stats
import com.prog7314.IdleLoadsheddingMobile.utils.FormatUtils
import kotlin.math.roundToInt

/**
 * Handles districts panel rendering and data binding.
 * Extracted from MainActivity to separate concerns.
 * 
 * Districts are the core economic units—each row tracks power output,
 * efficiency decay, and maintenance timers.
 */
class DistrictsPanelBinder(
    private val context: Context,
    private val formatUtils: FormatUtils
) {
    
    /**
     * Renders districts list within a panel.
     */
    fun bindData(panel: View, stats: UnityStatsSnapshot?) {
        val container = panel.findViewById<LinearLayout?>(R.id.list_districts) ?: return
        container.removeAllViews()
        
        val districts = stats?.districts.orEmpty()
        if (districts.isEmpty()) {
            // Density-aware padding keeps the empty state centered on all form factors
            val padding = (panel.resources.displayMetrics.density * 12).toInt()
            val emptyView = TextView(panel.context).apply {
                setText(R.string.districts_empty)
                textAlignment = View.TEXT_ALIGNMENT_CENTER
                setPadding(0, padding, 0, padding)
            }
            container.addView(emptyView)
            return
        }
        
        // Reuse a single inflater instance across all rows—minor optimization
        val inflater = LayoutInflater.from(panel.context)
        districts.forEach { district ->
            val row = inflater.inflate(R.layout.item_district_status, container, false)
            
            row.findViewById<TextView?>(R.id.text_district_name)?.text = district.name
            row.findViewById<TextView?>(R.id.text_district_level)?.text =
                context.getString(R.string.district_level_format, district.level)
            
            // Districts produce both power and money—display them side-by-side
            val powerText = context.getString(
                R.string.district_output_format,
                formatUtils.formatEnergy(district.powerPerSecond),
                formatUtils.formatEnergy(district.moneyPerSecond)
            )
            row.findViewById<TextView?>(R.id.text_district_output)?.text = powerText
            
            // Efficiency is a 0–1 float from Unity; clamp and scale it to a percentage
            val efficiencyPercent = (district.efficiency * 100).roundToInt().coerceIn(0, 100)
            row.findViewById<TextView?>(R.id.text_district_efficiency)?.text =
                context.getString(R.string.district_efficiency_format, efficiencyPercent)
            
            // Unity gives hours, we need seconds—multiply by 3600 and clamp negatives
            val idleSeconds = (district.hoursUntilMaintenance * 3600).coerceAtLeast(0.0)
            val idleText = if (idleSeconds <= 0.0) {
                context.getString(R.string.timestamp_unknown)
            } else {
                formatUtils.formatDuration(idleSeconds)
            }
            row.findViewById<TextView?>(R.id.text_district_idle_time)?.text =
                context.getString(R.string.district_idle_time_format, idleText)
            
            // Service button resets maintenance timers
            row.findViewById<Button?>(R.id.btn_district_service)?.setOnClickListener {
                PowerManager.serviceDistrict(district.id)
                Stats.requestUnitySnapshot()
            }
            
            // Upgrade button toggles based on whether district is maxed or affordable
            val upgradeButton = row.findViewById<Button?>(R.id.btn_district_upgrade)
            if (district.nextUpgradeCost <= 0.0) {
                upgradeButton?.isEnabled = false
                upgradeButton?.text = context.getString(R.string.district_upgrade_maxed)
            } else {
                upgradeButton?.isEnabled = true
                upgradeButton?.text = context.getString(
                    R.string.district_upgrade_cost_format,
                    formatUtils.formatEnergy(district.nextUpgradeCost)
                )
                upgradeButton?.setOnClickListener {
                    PowerManager.upgradeDistrict(district.id)
                    Stats.requestUnitySnapshot()
                }
            }
            
            container.addView(row)
        }
    }
}
