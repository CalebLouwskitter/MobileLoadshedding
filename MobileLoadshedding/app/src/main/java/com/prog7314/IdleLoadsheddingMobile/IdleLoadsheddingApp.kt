package com.prog7314.IdleLoadsheddingMobile

import android.app.Application
import com.prog7314.IdleLoadsheddingMobile.core.UpgradeStatsCoordinator
import com.prog7314.IdleLoadsheddingMobile.core.UpgradeSyncBridge

class IdleLoadsheddingApp : Application() {
    override fun onCreate() {
        super.onCreate()
        UpgradeStatsCoordinator.init(this)
        UpgradeSyncBridge.init(this)
    }
}
