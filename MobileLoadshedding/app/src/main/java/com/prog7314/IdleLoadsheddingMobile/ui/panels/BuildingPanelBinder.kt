package com.prog7314.IdleLoadsheddingMobile.ui.panels

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.prog7314.IdleLoadsheddingMobile.R
import com.prog7314.IdleLoadsheddingMobile.data.models.UnityStatsSnapshot
import com.prog7314.IdleLoadsheddingMobile.unity.PowerManager
import com.prog7314.IdleLoadsheddingMobile.unity.Stats
import com.prog7314.IdleLoadsheddingMobile.utils.FormatUtils

/**
 * Handles building/generator upgrade panel configuration and data binding.
 * Extracted from MainActivity (184 lines) to separate concerns.
 * 
 * This panel shows generator upgrades with:
 * - Visual feedback animations (flash, highlight, +1 indicators)
 * - Change detection to trigger animations only when stats actually change
 * - Click and upgrade handlers for each generator
 */
class BuildingPanelBinder(
    private val context: Context,
    private val formatUtils: FormatUtils
) {
    
    // Track previous generator stats to detect changes and trigger visual feedback
    private val previousGeneratorStats = mutableMapOf<String, Pair<Int, Double>>()
    
    /**
     * Configures building panel UI elements and event handlers.
     */
    fun configure(panel: View, stats: UnityStatsSnapshot?, onShowPanel: (Int) -> Unit) {
        // Tab navigation
        panel.findViewById<View?>(R.id.tab_electricity)?.setOnClickListener {
            onShowPanel(R.layout.panel_electricity_upgrade)
        }
        
        // Bind data
        bindData(panel, stats)
    }
    
    /**
     * Binds building/generator data to UI elements with animations.
     */
    fun bindData(panel: View, stats: UnityStatsSnapshot?) {
        val container = panel.findViewById<LinearLayout?>(R.id.list_building_upgrades)
        container?.removeAllViews()
        
        if (stats == null) {
            // Without stats we can't estimate levels—surface a friendly loading message
            val emptyText = TextView(context)
            emptyText.text = "Loading generator data..."
            emptyText.setTextColor(0xFF052E45.toInt())
            emptyText.textSize = 14f
            emptyText.setPadding(16, 16, 16, 16)
            container?.addView(emptyText)
            return
        }
        
        // Create upgrade rows for each potential generator (up to 4 generators in the scene)
        // We display all 4 slots even if some aren't active yet—this shows progression path
        val generatorIds = listOf("generator_1", "generator_2", "generator_3", "generator_4")
        
        // Calculate average level from aggregated data
        val avgLevel = if (stats.activeGenerators > 0) {
            stats.generatorLevelSum / stats.activeGenerators
        } else {
            1  // Default to level 1 if no generators are active yet
        }
        
        generatorIds.forEachIndexed { index, genId ->
            val generatorView = LayoutInflater.from(context).inflate(R.layout.view_upgrade_item, container, false)
            
            // Generator name is static
            generatorView.findViewById<TextView?>(R.id.text_upgrade_name)?.text = "Generator ${index + 1}"
            
            // Estimate output by dividing total production equally among active generators
            val estimatedOutput = if (stats.activeGenerators > 0) {
                stats.energyPerSecond / stats.activeGenerators
            } else {
                0.0
            }
            
            // CHANGE DETECTION: Compare current stats with previous snapshot
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
            outputTextView?.text = "~${formatUtils.formatEnergy(estimatedOutput)}/s"
            
            // VISUAL FEEDBACK: If level or output changed, animate to draw user's attention
            if (levelChanged || outputChanged) {
                animateChanges(
                    generatorView,
                    levelTextView,
                    outputTextView,
                    levelChanged,
                    outputChanged,
                    avgLevel,
                    previousStats
                )
            }
            
            // Upgrade button handler
            generatorView.findViewById<Button?>(R.id.btn_upgrade)?.apply {
                text = "Tap to Upgrade"
                setOnClickListener {
                    animateButtonPress(this)
                    PowerManager.upgradeGenerator(genId)
                    Stats.requestUnitySnapshot()
                    Toast.makeText(context, "Upgrading $genId...", Toast.LENGTH_SHORT).show()
                }
            }
            
            // Entire row is also clickable for burst energy (quick-tap mechanic)
            generatorView.setOnClickListener {
                generatorView.isPressed = true
                generatorView.postDelayed({ generatorView.isPressed = false }, 100)
                PowerManager.clickGenerator(genId)
                Stats.requestUnitySnapshot()
            }
            
            container?.addView(generatorView)
        }
    }
    
    /**
     * Animates visual feedback when generator stats change.
     */
    private fun animateChanges(
        generatorView: View,
        levelTextView: TextView?,
        outputTextView: TextView?,
        levelChanged: Boolean,
        outputChanged: Boolean,
        avgLevel: Int,
        previousStats: Pair<Int, Double>?
    ) {
        // 1. FLASH ANIMATION on the text views that changed
        val flashAnim = android.view.animation.AnimationUtils.loadAnimation(context, R.anim.value_change_flash)
        
        if (levelChanged && levelTextView != null) {
            levelTextView.startAnimation(flashAnim)
            
            // 2. +1 LEVEL INDICATOR: Show floating "+1" that fades upward
            val deltaIndicator = generatorView.findViewById<TextView?>(R.id.text_level_delta)
            deltaIndicator?.let { delta ->
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
                        .setStartDelay(200)
                        .withEndAction {
                            delta.translationY = 0f  // Reset position
                        }
                        .start()
                }
            }
        }
        
        if (outputChanged && outputTextView != null) {
            outputTextView.startAnimation(flashAnim)
        }
        
        // 3. HIGHLIGHT OVERLAY: Green flash on entire row
        val highlightView = generatorView.findViewById<View?>(R.id.view_update_highlight)
        highlightView?.let { highlight ->
            highlight.alpha = 0.3f
            highlight.animate()
                .alpha(0f)
                .setDuration(800)
                .setStartDelay(100)
                .start()
        }
    }
    
    /**
     * Animates button press feedback.
     */
    private fun animateButtonPress(button: Button) {
        button.animate()
            .scaleX(0.95f)
            .scaleY(0.95f)
            .setDuration(50)
            .withEndAction {
                button.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(150)
                    .start()
            }
            .start()
    }
}
