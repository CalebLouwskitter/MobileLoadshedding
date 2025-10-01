package com.prog7314.IdleLoadsheddingMobile.data.models

data class UnityEventPayload(
    val id: String,
    val title: String,
    val description: String,
    val isStarting: Boolean,
    val durationSeconds: Float,
    val productionMultiplier: Float,
    val timestampUtc: String
)
