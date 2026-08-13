package com.hlrms.mobile.data.remote

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object TransferApiClient {

    private const val BASE_URL =
        "https://uncorrupt-swifter-spruce.ngrok-free.dev/"

    private val retrofit by lazy {

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(
                ApiClient.authenticatedClient()
            )
            .addConverterFactory(
                GsonConverterFactory.create()
            )
            .build()
    }

    val transferApi: TransferApi by lazy {

        retrofit.create(
            TransferApi::class.java
        )
    }
}