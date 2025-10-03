package com.prog7314.IdleLoadsheddingMobile.data.api

import com.prog7314.IdleLoadsheddingMobile.data.models.BackendStatsSnapshot
import com.prog7314.IdleLoadsheddingMobile.data.models.BackendStatsSummary
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface StatsApi {
    @POST("game/sync")  // Changed from "stats" to match backend route
    fun postSnapshot(@Body snapshot: BackendStatsSnapshot): Call<Void>

    @GET("game/leaderboard")  // Changed from "stats/summary" to match backend route
    fun fetchSummary(): Call<BackendStatsSummary>
}
