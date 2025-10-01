package com.prog7314.IdleLoadsheddingMobile.data.api

import com.prog7314.IdleLoadsheddingMobile.data.models.LeaderboardEntryResponse
import com.prog7314.IdleLoadsheddingMobile.data.models.UnityLeaderboardPayload
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface LeaderboardApi {
    @POST("game/leaderboard")
    fun postLeaderboard(@Body payload: UnityLeaderboardPayload): Call<Void>

    @GET("game/leaderboard")
    fun fetchLeaderboard(): Call<List<LeaderboardEntryResponse>>
}
