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
        // Back arrow in header - behaves like tapping the close button on the modal
        panel.findViewById<View?>(R.id.btn_back_tasks)?.setOnClickListener {
            panel.rootView.findViewById<View?>(R.id.close_button)?.performClick()
        }

        val empty = panel.findViewById<TextView?>(R.id.text_tasks_empty)
        val container = panel.findViewById<LinearLayout?>(R.id.tasks_list_container) ?: return

        if (eventHistory.isEmpty()) {
            // For now, show placeholder rows so the UI matches the designed layout
            empty?.isVisible = false
            container.isVisible = true
            renderPlaceholderTasks(container)
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

            // Main task description line with active/inactive prefix
            val descriptionView = row.findViewById<TextView?>(R.id.task_description)
            descriptionView?.text = when {
                event.isStarting && currentActiveEvent?.id == event.id ->
                    activity.getString(R.string.tasks_event_active, event.title)
                event.isStarting ->
                    activity.getString(R.string.tasks_event_active, event.title)
                else ->
                    activity.getString(R.string.tasks_event_inactive, event.title)
            }

            // Reward line: show formatted timestamp as a pseudo-reward badge
            val rewardView = row.findViewById<TextView?>(R.id.task_reward)
            rewardView?.text = formatUtils.formatTimestamp(event.timestampUtc)

            // Visual indicator: checkbox-style state mapped to the circular check icon
            val icon = row.findViewById<ImageView?>(R.id.task_check_icon)
            icon?.setImageResource(
                if (event.isStarting) android.R.drawable.checkbox_on_background else android.R.drawable.checkbox_off_background
            )

            container.addView(row)
        }
    }

    /**
     * Temporary placeholder content for when there is no real event history yet.
     * This keeps the tasks panel looking like the final UI during development.
     */
    private fun renderPlaceholderTasks(container: LinearLayout) {
        container.removeAllViews()

        repeat(3) {
            val row = LayoutInflater.from(activity).inflate(R.layout.view_task_item, container, false)

            row.findViewById<TextView?>(R.id.task_description)?.text =
                "Upgrade Storage to level 6"

            row.findViewById<TextView?>(R.id.task_reward)?.text = "R 20,000"

            val icon = row.findViewById<ImageView?>(R.id.task_check_icon)
            icon?.setImageResource(android.R.drawable.checkbox_on_background)

            container.addView(row)
        }
    }
}
