package com.prog7314.IdleLoadsheddingMobile.GameUI

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.prog7314.IdleLoadsheddingMobile.core.UpgradeStatsCoordinator
import com.prog7314.IdleLoadsheddingMobile.data.models.UpgradeStats
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class UpgradeStatsViewModel(application: Application) : AndroidViewModel(application) {

    val stats: StateFlow<UpgradeStats> = UpgradeStatsCoordinator.statsState

    fun buyDamageUpgrade(cost: Long) {
        viewModelScope.launch {
            runCatching { UpgradeStatsCoordinator.buyDamageUpgrade(cost) }
                .onFailure { Log.w(TAG, "Failed to buy upgrade", it) }
        }
    }

    fun grantCurrency(amount: Long) {
        viewModelScope.launch {
            runCatching { UpgradeStatsCoordinator.grantCurrency(amount) }
                .onFailure { Log.w(TAG, "Failed to grant currency", it) }
        }
    }

    companion object {
        private const val TAG = "UpgradeStatsVM"
    }
}
