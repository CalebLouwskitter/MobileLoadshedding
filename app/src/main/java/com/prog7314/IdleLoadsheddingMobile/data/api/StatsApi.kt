package com.prog7314.IdleLoadsheddingMobile.data.api

import com.prog7314.IdleLoadsheddingMobile.data.models.BackendStatsSnapshot
import com.prog7314.IdleLoadsheddingMobile.data.models.BackendStatsSummary
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface StatsApi {
    @POST("stats")
    fun postSnapshot(@Body snapshot: BackendStatsSnapshot): Call<Void>

    @GET("stats/summary")
    fun fetchSummary(): Call<BackendStatsSummary>
}
