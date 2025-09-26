package com.prog7314.IdleLoadsheddingMobile.networking

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val BASE_URL = "https://your.api.base.url/"

    val instance: ApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        retrofit.create(ApiService::class.java)
    }
}

// Define your API service interface here
// For example:
// interface ApiService {
//     @GET("data")
//     suspend fun getData(): Response<YourDataModel>
// }
