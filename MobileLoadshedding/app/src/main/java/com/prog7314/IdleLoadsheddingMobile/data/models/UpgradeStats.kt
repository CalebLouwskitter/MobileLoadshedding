package com.prog7314.IdleLoadsheddingMobile.data.models

/**
 * UI-friendly representation of the upgrade/currency tuple we need to keep in
 * sync between Android and Unity. The `lastUpdatedEpochMillis` value is useful
 * for debugging or optimistic updates if the UI needs to highlight freshness.
 */
data class UpgradeStats(
    val damageLevel: Int,
    val currency: Long,
    val lastUpdatedEpochMillis: Long
)
