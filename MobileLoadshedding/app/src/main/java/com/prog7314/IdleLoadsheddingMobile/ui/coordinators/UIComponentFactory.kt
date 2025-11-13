package com.prog7314.IdleLoadsheddingMobile.ui.coordinators

import android.app.Activity
import android.content.Context
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.prog7314.IdleLoadsheddingMobile.R
import com.prog7314.IdleLoadsheddingMobile.ui.HudManager
import com.prog7314.IdleLoadsheddingMobile.ui.PanelManager
import com.prog7314.IdleLoadsheddingMobile.ui.panels.*
import com.prog7314.IdleLoadsheddingMobile.utils.FormatUtils
import com.prog7314.IdleLoadsheddingMobile.utils.LocaleManager

/**
 * Factory for creating and initializing all UI coordinators and binders.
 * 
 * This class handles the complex initialization dance of creating managers,
 * binders, and coordinators with their interdependencies. It reduces 
 * MainActivity's onCreate from 100+ lines to a single factory call.
 * 
 * WHY THIS EXISTS:
 * - Centralizes complex initialization logic
 * - Eliminates repetitive binder instantiation code
 * - Makes dependency injection easier if we add DI later
 * - Provides single place to modify initialization order
 * - Reduces MainActivity onCreate to essential setup only
 */
class UIComponentFactory(
    private val activity: Activity,
    private val auth: FirebaseAuth
) {
    private val context: Context get() = activity
    
    /**
     * Data class holding all initialized UI components.
     */
    data class UIComponents(
        val formatUtils: FormatUtils,
        val localeManager: LocaleManager,
        val stateManager: StateManager,
        val panelManager: PanelManager,
        val hudManager: HudManager,
        val panelCoordinator: PanelCoordinator,
        val observerCoordinator: ObserverCoordinator
    )
    
    /**
     * Initialize all UI components with proper dependency order.
     * 
     * @param hudTextViews Map of HUD TextView IDs to their views
     * @param modalContainer Container for modal panels
     * @param modalScrim Backdrop for modal panels
     * @param onStateChanged Callback when game state updates
     * @return Initialized UI components
     */
    fun createComponents(
        hudTextViews: HudTextViews,
        modalContainer: android.widget.FrameLayout,
        modalScrim: View,
        onStateChanged: () -> Unit
    ): UIComponents {
        
        // 1. Create utilities (no dependencies)
        val formatUtils = FormatUtils(context)
        val localeManager = LocaleManager(context)
        
        // 2. Create state manager (no dependencies)
        val stateManager = StateManager()
        
        // 3. Create managers (depend on utilities)
        val panelManager = PanelManager(context, modalContainer, modalScrim)
        val hudManager = HudManager(
            context,
            formatUtils,
            hudTextViews.textEnergyAmount,
            hudTextViews.textEnergyPerSecond,
            hudTextViews.textActiveGenerators,
            hudTextViews.textEventStatus,
            hudTextViews.textSafeIdleHours
        )
        
        // 4. Create panel binders (depend on utilities and auth)
        val settingsPrefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        
        val districtsPanelBinder = DistrictsPanelBinder(context, formatUtils)
        val electricityPanelBinder = ElectricityPanelBinder(context, formatUtils, districtsPanelBinder)
        val buildingPanelBinder = BuildingPanelBinder(context, formatUtils)
        val profilePanelBinder = ProfilePanelBinder(context, formatUtils, auth)
        val leaderboardPanelBinder = LeaderboardPanelBinder(activity, formatUtils)
        val friendsPanelBinder = FriendsPanelBinder(activity as AppCompatActivity, formatUtils)
        val tasksPanelBinder = TasksPanelBinder(activity, formatUtils)
        val profileEditPanelBinder = ProfileEditPanelBinder(activity, auth)
        val settingsPanelBinder = SettingsPanelBinder(
            activity,
            auth,
            settingsPrefs,
            formatUtils,
            localeManager
        ) {
            // Clear state on logout
            stateManager.clearAll()
        }
        
        // 5. Create coordinators (depend on managers and binders)
        val panelCoordinator = PanelCoordinator(
            stateManager,
            buildingPanelBinder,
            districtsPanelBinder,
            electricityPanelBinder,
            profilePanelBinder,
            settingsPanelBinder,
            leaderboardPanelBinder,
            friendsPanelBinder,
            tasksPanelBinder,
            profileEditPanelBinder
        )
        
        val observerCoordinator = ObserverCoordinator(stateManager, onStateChanged)
        
        return UIComponents(
            formatUtils,
            localeManager,
            stateManager,
            panelManager,
            hudManager,
            panelCoordinator,
            observerCoordinator
        )
    }
    
    /**
     * Data class for passing HUD TextView references.
     */
    data class HudTextViews(
        val textEnergyAmount: android.widget.TextView,
        val textEnergyPerSecond: android.widget.TextView,
        val textActiveGenerators: android.widget.TextView,
        val textEventStatus: android.widget.TextView,
        val textSafeIdleHours: android.widget.TextView
    )
}
