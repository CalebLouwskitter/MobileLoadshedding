package com.prog7314.IdleLoadsheddingMobile.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.LayoutRes
import com.prog7314.IdleLoadsheddingMobile.R

/**
 * Manages modal panel lifecycle (show, hide, inflate).
 * Extracted from MainActivity to separate concerns and reduce code complexity.
 * 
 * Handles:
 * - Panel inflation and container management
 * - Modal scrim visibility
 * - Close button wiring
 * - Current panel state tracking
 */
class PanelManager(
    private val context: Context,
    private val modalContainer: FrameLayout,
    private val modalScrim: View
) {
    
    var currentPanelLayoutId: Int? = null
        private set
    
    var currentPanelView: View? = null
        private set
    
    /**
     * Shows a panel by inflating its layout and adding it to the modal container.
     * 
     * @param layoutResId The layout resource ID to inflate
     * @param onConfigurePanel Optional callback to configure the panel after inflation
     * @return The inflated panel view
     */
    fun showPanel(
        @LayoutRes layoutResId: Int,
        onConfigurePanel: ((View) -> Unit)? = null
    ): View {
        // Inflate a fresh copy each time to avoid stale state bleeding between panels
        val panelView = LayoutInflater.from(context).inflate(layoutResId, modalContainer, false)
        
        modalContainer.removeAllViews()
        modalContainer.addView(panelView)
        modalContainer.visibility = View.VISIBLE
        modalScrim.visibility = View.VISIBLE
        
        currentPanelLayoutId = layoutResId
        currentPanelView = panelView
        
        // Wire up close button if present
        panelView.findViewById<View?>(R.id.close_button)?.setOnClickListener {
            hidePanel()
        }
        
        // Allow caller to configure the panel
        onConfigurePanel?.invoke(panelView)
        
        return panelView
    }
    
    /**
     * Hides the currently visible panel and clears state.
     */
    fun hidePanel() {
        modalContainer.removeAllViews()
        modalContainer.visibility = View.GONE
        modalScrim.visibility = View.GONE
        currentPanelLayoutId = null
        currentPanelView = null
    }
    
    /**
     * Checks if a specific panel is currently visible.
     */
    fun isPanelVisible(@LayoutRes layoutResId: Int): Boolean {
        return currentPanelLayoutId == layoutResId
    }
    
    /**
     * Gets the current panel view if it matches the given layout ID.
     */
    fun getCurrentPanelViewIfMatches(@LayoutRes layoutResId: Int): View? {
        return if (currentPanelLayoutId == layoutResId) currentPanelView else null
    }
    
    /**
     * Checks if any panel is currently visible.
     */
    fun isAnyPanelVisible(): Boolean {
        return currentPanelLayoutId != null
    }
    
    /**
     * Gets the currently visible panel view.
     */
    fun getCurrentPanel(): View? {
        return currentPanelView
    }
    
    /**
     * Gets the currently visible panel layout ID.
     */
    fun getCurrentLayoutId(): Int? {
        return currentPanelLayoutId
    }
}
