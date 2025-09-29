package com.prog7314.IdleLoadsheddingMobile.networking

import com.prog7314.IdleLoadsheddingMobile.unity.BackendClient

object RetrofitClient {
    val instance: ApiService by lazy {
        BackendClient.retrofit.create(ApiService::class.java)
    }
}
