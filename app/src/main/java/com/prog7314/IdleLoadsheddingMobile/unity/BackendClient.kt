package com.prog7314.IdleLoadsheddingMobile.unity

import com.prog7314.IdleLoadsheddingMobile.BuildConfig
import com.prog7314.IdleLoadsheddingMobile.networking.AuthInterceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

internal object BackendClient {
    private const val DEFAULT_BASE_URL = "https://api.idleloadshed.example/"

    private val baseUrl: String
        get() = BuildConfig.BACKEND_BASE_URL.takeIf { it.isNotBlank() } ?: DEFAULT_BASE_URL

    private val httpClient: OkHttpClient by lazy {
        val builder = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor)

        if (BuildConfig.DEBUG) {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            builder.addInterceptor(logging)
        }

        builder.build()
    }

    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(baseUrl.ensureTrailingSlash())
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    inline fun <reified T> create(): T = retrofit.create(T::class.java)

    private fun String.ensureTrailingSlash(): String = if (endsWith('/')) this else "$this/"
}
