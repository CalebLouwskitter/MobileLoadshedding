package com.prog7314.IdleLoadsheddingMobile.core

import com.prog7314.IdleLoadsheddingMobile.BuildConfig //Android, 2025.
import com.prog7314.IdleLoadsheddingMobile.networking.AuthInterceptor
import okhttp3.OkHttpClient //Square, 2025.
import okhttp3.logging.HttpLoggingInterceptor //Square, 2025.
import retrofit2.Retrofit //Square, 2025.
import retrofit2.converter.gson.GsonConverterFactory //Square, 2025.

/**
 * BackendClient configures and provides the Retrofit instance for API communication.
 */
internal object BackendClient { //Square, 2025.
    private const val DEFAULT_BASE_URL = "http://idle-loadshedding-api-7711.southafricanorth.azurecontainer.io:8080/api/"

    private val baseUrl: String
        get() = BuildConfig.BACKEND_BASE_URL.takeIf { it.isNotBlank() } ?: DEFAULT_BASE_URL //Android, 2025.

    private val httpClient: OkHttpClient by lazy { //Square, 2025.
        val builder = OkHttpClient.Builder() //Square, 2025.
            .addInterceptor(AuthInterceptor) //Square, 2025.

        if (BuildConfig.DEBUG) { //Android, 2025.
            val logging = HttpLoggingInterceptor().apply { //Square, 2025.
                level = HttpLoggingInterceptor.Level.BODY //Square, 2025.
            }
            builder.addInterceptor(logging) //Square, 2025.
        }

        builder.build()
    }

    val retrofit: Retrofit by lazy { //Square, 2025.
        Retrofit.Builder() //Square, 2025.
            .baseUrl(baseUrl.ensureTrailingSlash())
            .client(httpClient) //Square, 2025.
            .addConverterFactory(GsonConverterFactory.create()) //Square, 2025.
            .build()
    }

    inline fun <reified T> create(): T = retrofit.create(T::class.java) //Square, 2025. //Kotlin, 2025.

    private fun String.ensureTrailingSlash(): String = if (endsWith('/')) this else "$this/" //Kotlin, 2025.
}

/**
 * Reference List:
 * - Android, 2025. Android Developers - BuildConfig. [online] Available at:
 *   https://developer.android.com/reference/tools/gradle-api/com/android/build/api/variant/BuildConfigField
 *   [Accessed 1 Oct. 2025].
 * - Square, 2025. Retrofit - Type-safe HTTP client for Android and Java. [online] Available at:
 *   https://square.github.io/retrofit/ [Accessed 1 Oct. 2025].
 * - Square, 2025. OkHttp - An HTTP client for Android and Java. [online] Available at:
 *   https://square.github.io/okhttp/ [Accessed 1 Oct. 2025].
 * - Kotlin, 2025. Kotlin Language Documentation - Inline Functions. [online] Available at:
 *   https://kotlinlang.org/docs/inline-functions.html [Accessed 1 Oct. 2025].
 */
