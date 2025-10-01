package com.prog7314.IdleLoadsheddingMobile.data.api

import com.prog7314.IdleLoadsheddingMobile.data.models.FriendsPayload
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface FriendsApi {
    @POST("social/friends")
    fun postFriends(@Query("playerId") playerId: String?, @Body payload: FriendsPayload): Call<Void>

    @GET("social/friends")
    fun fetchFriends(@Query("playerId") playerId: String?): Call<FriendsPayload>
}
