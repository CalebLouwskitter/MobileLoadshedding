package com.prog7314.IdleLoadsheddingMobile.networking

import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

object AuthInterceptor : Interceptor {
    private const val AUTH_HEADER = "Authorization"

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()

        val token = runBlocking { FirebaseIdTokenProvider.getToken() }
        val authenticatedRequest = if (!token.isNullOrBlank()) {
            original.newBuilder()
                .header(AUTH_HEADER, "Bearer $token")
                .build()
        } else {
            original
        }

        return chain.proceed(authenticatedRequest)
    }
}
