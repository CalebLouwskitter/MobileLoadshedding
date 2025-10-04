package com.prog7314.IdleLoadsheddingMobile.data.api

import com.prog7314.IdleLoadsheddingMobile.data.models.LeaderboardEntryResponse
import com.prog7314.IdleLoadsheddingMobile.data.models.LeaderboardSubmission
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface LeaderboardApi {
    @POST("game/leaderboard")
    fun submitScore(@Body submission: LeaderboardSubmission): Call<Void>

    @GET("game/leaderboard")
    fun fetchLeaderboard(): Call<List<LeaderboardEntryResponse>>
}
