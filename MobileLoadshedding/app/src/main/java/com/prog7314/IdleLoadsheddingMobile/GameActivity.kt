package com.prog7314.IdleLoadsheddingMobile

import android.os.Bundle //Android, 2025.
import com.prog7314.IdleLoadsheddingMobile.GameUI.GameStatsFragment
import com.prog7314.IdleLoadsheddingMobile.core.UnityBridge
import com.prog7314.IdleLoadsheddingMobile.unity.Stats
import com.unity3d.player.UnityPlayerGameActivity //Unity Technologies, 2025.

/**
 * GameActivity extends UnityPlayerGameActivity to embed the Unity game
 * and display native UI overlays for stats and other features.
 */
class GameActivity : UnityPlayerGameActivity() { //Unity Technologies, 2025.
    
    override fun onCreate(savedInstanceState: Bundle?) { //Android, 2025.
        super.onCreate(savedInstanceState)
        
        // UnityPlayerGameActivity already sets up the Unity view
        // We just need to add our stats overlay on top
        
        // Add stats overlay fragment on top of Unity
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .add(android.R.id.content, GameStatsFragment.newInstance())
                .commit()
        }

        // Ensure Unity HUD is hidden and trigger an initial stats sync once the Unity view is ready
        window.decorView.post {
            UnityBridge.hideUnityHud()
            Stats.requestUnitySnapshot()
        }
    }
}

/**
 * Reference List:
 * - Android, 2025. Android Developers - Activity. [online] Available at:
 *   https://developer.android.com/reference/android/app/Activity [Accessed 1 Oct. 2025].
 * - Unity Technologies, 2025. Unity for Android - UnityPlayerGameActivity. [online] Available at:
 *   https://docs.unity3d.com/Manual/android-plugins.html [Accessed 1 Oct. 2025].
 */



