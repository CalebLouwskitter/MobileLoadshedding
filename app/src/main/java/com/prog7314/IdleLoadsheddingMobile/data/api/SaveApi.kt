package com.prog7314.IdleLoadsheddingMobile.data.api

import com.prog7314.IdleLoadsheddingMobile.data.models.BackendSavePayload
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface SaveApi {
    @POST("game/sync")
    fun syncSave(@Body payload: BackendSavePayload): Call<Void>
}
