package com.prog7314.IdleLoadsheddingMobile.ui.panels

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import com.prog7314.IdleLoadsheddingMobile.R
import com.prog7314.IdleLoadsheddingMobile.data.models.UnityEventPayload
import com.prog7314.IdleLoadsheddingMobile.utils.FormatUtils

/**
 * Handles tasks panel configuration and data binding.
 * Extracted from MainActivity to separate concerns.
 */
class TasksPanelBinder(
    private val activity: Activity,
    private val formatUtils: FormatUtils
) {
    
    /**
     * Configures and binds tasks panel with event history.
     */
    fun configure(
        panel: View,
        eventHistory: List<UnityEventPayload>,
        currentActiveEvent: UnityEventPayload?
    ) {
        val empty = panel.findViewById<TextView?>(R.id.text_tasks_empty)
        val container = panel.findViewById<LinearLayout?>(R.id.tasks_list_container) ?: return

        if (eventHistory.isEmpty()) {
            // Show empty state
            empty?.isVisible = true
            container.isVisible = false
            container.removeAllViews()
        } else {
            empty?.isVisible = false
            container.isVisible = true
            renderEventHistory(container, eventHistory, currentActiveEvent)
        }
    }
    
    /**
     * Renders event history into the container.
     * Cap at 10 events to avoid scroll lag.
     */
    private fun renderEventHistory(
        container: LinearLayout,
        history: List<UnityEventPayload>,
        currentActiveEvent: UnityEventPayload?
    ) {
        container.removeAllViews()
        history.take(10).forEach { event ->
            val row = LayoutInflater.from(activity).inflate(R.layout.view_task_item, container, false)
            
            // Mark active events with special prefix
            row.findViewById<TextView?>(R.id.text_task_title)?.text = when {
                event.isStarting && currentActiveEvent?.id == event.id ->
                    activity.getString(R.string.tasks_event_active, event.title)
                event.isStarting ->
                    activity.getString(R.string.tasks_event_active, event.title)
                else ->
                    activity.getString(R.string.tasks_event_inactive, event.title)
            }

            // Pack description, multiplier, and duration into detail line
            val detail = buildString {
                append(event.description)
                append(" • ")
                append(activity.getString(R.string.tasks_event_multiplier, event.productionMultiplier))
                append(" • ")
                append(activity.getString(R.string.tasks_event_duration, formatUtils.formatDuration(event.durationSeconds.toDouble())))
            }
            row.findViewById<TextView?>(R.id.text_task_detail)?.text = detail
            row.findViewById<TextView?>(R.id.text_task_badge)?.text = formatUtils.formatTimestamp(event.timestampUtc)
            
            // Visual indicator: checkbox for active/inactive
            val icon = row.findViewById<ImageView?>(R.id.image_task_state)
            icon?.setImageResource(
                if (event.isStarting) android.R.drawable.checkbox_on_background else android.R.drawable.checkbox_off_background
            )
            container.addView(row)
        }
    }
}
