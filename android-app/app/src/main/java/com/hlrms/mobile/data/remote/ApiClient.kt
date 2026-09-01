package com.hlrms.mobile.data.remote

import android.content.Context
import com.hlrms.mobile.data.local.SessionManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {

    private const val BASE_URL =
        "https://uncorrupt-swifter-spruce.ngrok-free.dev/"

    private lateinit var sessionManager:
            SessionManager

    fun initialize(
        context: Context
    ) {

        if (::sessionManager.isInitialized) {
            return
        }

        sessionManager =
            SessionManager(
                context.applicationContext
            )
    }

    private val loggingInterceptor by lazy {

        HttpLoggingInterceptor().apply {

            level =
                HttpLoggingInterceptor
                    .Level
                    .BASIC

            redactHeader(
                "Authorization"
            )
        }
    }

    /*
     * Client منفصل للـAuth حتى لا يدخل refresh نفسه
     * في حلقة Authenticator.
     */
    private val authHttpClient by lazy {

        OkHttpClient.Builder()
            .addInterceptor(
                loggingInterceptor
            )
            .build()
    }

    private val authRetrofit by lazy {

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(authHttpClient)
            .addConverterFactory(
                GsonConverterFactory.create()
            )
            .build()
    }

    val authApi: AuthApi by lazy {

        checkInitialized()

        authRetrofit.create(
            AuthApi::class.java
        )
    }

    private val authenticatedHttpClient by lazy {

        checkInitialized()

        OkHttpClient.Builder()
            .authenticator(
                TokenAuthenticator(
                    sessionManager =
                        sessionManager,

                    authApiProvider = {
                        authApi
                    }
                )
            )
            .addInterceptor(
                loggingInterceptor
            )
            .build()
    }

    private val authenticatedRetrofit by lazy {

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(
                authenticatedHttpClient
            )
            .addConverterFactory(
                GsonConverterFactory.create()
            )
            .build()
    }

    val requestApi: RequestApi by lazy {

        checkInitialized()

        authenticatedRetrofit.create(
            RequestApi::class.java
        )
    }

    fun authenticatedClient():
            OkHttpClient {

        checkInitialized()

        return authenticatedHttpClient
    }

    private fun checkInitialized() {

        check(
            ::sessionManager.isInitialized
        ) {
            "ApiClient.initialize(context) must be called first"
        }
    }
}