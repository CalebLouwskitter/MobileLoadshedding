package com.prog7314.IdleLoadsheddingMobile.GameUI

import android.os.Bundle //Android, 2025.
import android.util.Log //Android, 2025.
import android.view.LayoutInflater //Android, 2025.
import android.view.View //Android, 2025.
import android.view.ViewGroup //Android, 2025.
import android.widget.TextView //Android, 2025.
import androidx.fragment.app.Fragment //AndroidX, 2025.
import com.prog7314.IdleLoadsheddingMobile.R
import com.prog7314.IdleLoadsheddingMobile.data.models.UnityStatsSnapshot
import com.prog7314.IdleLoadsheddingMobile.unity.Stats
import java.text.SimpleDateFormat //Java, 2025.
import java.util.Date //Java, 2025.
import java.util.Locale //Java, 2025.

/**
 * GameStatsFragment displays real-time game statistics received from Unity.
 * This provides a native Android UI overlay for the game stats.
 */
class GameStatsFragment : Fragment(), Stats.Observer { //AndroidX, 2025.

    companion object {
        private const val TAG = "GameStatsFragment"
        
        /**
         * Factory method to create a new instance of this fragment.
         */
        @JvmStatic
        fun newInstance(): GameStatsFragment {
            return GameStatsFragment()
        }
    }

    private var tvCurrentEnergy: TextView? = null
    private var tvTotalEnergy: TextView? = null
    private var tvEnergyPerSecond: TextView? = null
    private var tvActiveGenerators: TextView? = null
    private var tvGeneratorLevelSum: TextView? = null
    private var tvLastUpdated: TextView? = null
    private var tvTapCounter: TextView? = null
    private var tvLastTapAmount: TextView? = null

    private val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault()) //Java, 2025.

    override fun onCreateView(
        inflater: LayoutInflater, //Android, 2025.
        container: ViewGroup?, //Android, 2025.
        savedInstanceState: Bundle? //Android, 2025.
    ): View? { //Android, 2025.
        return inflater.inflate(R.layout.fragment_game_stats, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) { //AndroidX, 2025.
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        tvCurrentEnergy = view.findViewById(R.id.tvCurrentEnergy)
        tvTotalEnergy = view.findViewById(R.id.tvTotalEnergy)
        tvEnergyPerSecond = view.findViewById(R.id.tvEnergyPerSecond)
        tvActiveGenerators = view.findViewById(R.id.tvActiveGenerators)
        tvGeneratorLevelSum = view.findViewById(R.id.tvGeneratorLevelSum)
        tvLastUpdated = view.findViewById(R.id.tvLastUpdated)
        tvTapCounter = view.findViewById(R.id.tvTapCounter)
        tvLastTapAmount = view.findViewById(R.id.tvLastTapAmount)

        // Register as observer for stats updates
        Stats.addObserver(this)
        
        // Request immediate stats update from Unity
        Stats.requestUnitySnapshot()
    }

    override fun onDestroyView() { //AndroidX, 2025.
        super.onDestroyView()
        Stats.removeObserver(this)
        
        // Clear references to prevent memory leaks
        tvCurrentEnergy = null
        tvTotalEnergy = null
        tvEnergyPerSecond = null
        tvActiveGenerators = null
        tvGeneratorLevelSum = null
        tvLastUpdated = null
        tvTapCounter = null
        tvLastTapAmount = null
    }

    override fun onStatsUpdated(snapshot: UnityStatsSnapshot) {
        Log.d(TAG, "=== UI UPDATE ===")
        Log.d(TAG, "Energy: ${snapshot.currentEnergy}, EPS: ${snapshot.energyPerSecond}")
        
        // Update UI on main thread
        view?.post {
            tvCurrentEnergy?.text = String.format(Locale.getDefault(), "%.2f", snapshot.currentEnergy)
            tvTotalEnergy?.text = String.format(Locale.getDefault(), "%.2f", snapshot.totalEnergyGenerated)
            tvEnergyPerSecond?.text = String.format(Locale.getDefault(), "%.2f", snapshot.energyPerSecond)
            tvActiveGenerators?.text = snapshot.activeGenerators.toString()
            tvGeneratorLevelSum?.text = snapshot.generatorLevelSum.toString()
            tvLastUpdated?.text = "Last updated: ${dateFormat.format(Date())}" //Java, 2025.
            
            Log.d(TAG, "UI TextViews updated successfully")
        }
    }

    override fun onEnergyTapped(amount: Double) {
        Log.d(TAG, "=== TAP EVENT === Amount: $amount, Total: ${Stats.getTotalTapCount()}")
        
        view?.post {
            tvTapCounter?.text = "Taps: ${Stats.getTotalTapCount()}"
            tvLastTapAmount?.text = "Last tap: +${String.format(Locale.getDefault(), "%.1f", amount)}"
            
            // Add visual feedback - flash the current energy display
            tvCurrentEnergy?.apply {
                alpha = 0.5f
                animate().alpha(1.0f).setDuration(200).start()
            }
            
            Log.d(TAG, "Tap UI updated")
        }
    }

    override fun onEnergyBurst(amount: Double) {
        Log.d(TAG, "=== BURST EVENT === Amount: $amount")
        
        view?.post {
            tvLastTapAmount?.text = "Energy burst: +${String.format(Locale.getDefault(), "%.1f", amount)}"
            
            // Add more prominent visual feedback for burst energy
            tvCurrentEnergy?.apply {
                scaleX = 0.8f
                scaleY = 0.8f
                animate()
                    .scaleX(1.2f)
                    .scaleY(1.2f)
                    .setDuration(150)
                    .withEndAction {
                        animate().scaleX(1.0f).scaleY(1.0f).setDuration(150).start()
                    }
                    .start()
            }
            
            Log.d(TAG, "Burst UI updated")
        }
    }
}

/**
 * Reference List:
 * - Android, 2025. Android Developers - Fragment. [online] Available at:
 *   https://developer.android.com/guide/fragments [Accessed 1 Oct. 2025].
 * - AndroidX, 2025. AndroidX Fragment Library. [online] Available at:
 *   https://developer.android.com/jetpack/androidx/releases/fragment [Accessed 1 Oct. 2025].
 * - Java, 2025. Java Platform Standard Edition Documentation. [online] Available at:
 *   https://docs.oracle.com/en/java/javase/ [Accessed 1 Oct. 2025].
 */
