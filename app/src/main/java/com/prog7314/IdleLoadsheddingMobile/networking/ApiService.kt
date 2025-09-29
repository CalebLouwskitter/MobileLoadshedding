package com.prog7314.IdleLoadsheddingMobile.networking

import retrofit2.Response
import retrofit2.http.GET

interface ApiService {
    @GET("health")
    suspend fun getHealth(): Response<HealthResponse>
}

data class HealthResponse(
    val status: String,
    val utc: String?
)
