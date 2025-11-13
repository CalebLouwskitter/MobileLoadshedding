package com.prog7314.IdleLoadsheddingMobile.data.api

import com.prog7314.IdleLoadsheddingMobile.data.models.FriendResponseDto
import com.prog7314.IdleLoadsheddingMobile.data.models.FriendsSyncRequest
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface FriendsApi {
    @POST("social/friends/sync")
    fun syncFriends(@Body payload: FriendsSyncRequest): Call<Void>

    @GET("social/friends")
    fun fetchFriends(): Call<List<FriendResponseDto>>
    
    @GET("social/friends/resolve/{friendCode}")
    fun resolveFriendCode(@Path("friendCode") friendCode: String): Call<Map<String, String>>
}
