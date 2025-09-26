package com.prog7314.IdleLoadsheddingMobile.networking

import retrofit2.Response
import retrofit2.http.GET

interface ApiService {
    @GET("status")
    suspend fun getStatus(): Response<Unit>
}
