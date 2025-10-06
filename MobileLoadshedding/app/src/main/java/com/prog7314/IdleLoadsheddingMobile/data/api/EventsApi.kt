package com.prog7314.IdleLoadsheddingMobile.data.api

import com.prog7314.IdleLoadsheddingMobile.data.models.UnityEventPayload
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface EventsApi {
    @POST("events")
    fun recordEvent(@Body payload: UnityEventPayload): Call<Void>
}
