package com.hlrms.mobile.data.remote

import com.hlrms.mobile.data.remote.request.CreateRequestDto
import com.hlrms.mobile.data.remote.request.PageResponseDto
import com.hlrms.mobile.data.remote.request.RequestResponseDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface RequestApi {

    @POST("api/v1/requests")
    suspend fun createRequest(
        @Header("Authorization")
        authorization: String,

        @Header("Idempotency-Key")
        idempotencyKey: String,

        @Body
        request: CreateRequestDto
    ): RequestResponseDto

    @GET("api/v1/requests/{id}")
    suspend fun getRequestById(
        @Header("Authorization")
        authorization: String,

        @Path("id")
        requestId: String
    ): RequestResponseDto

    @GET("api/v1/requests")
    suspend fun getRequests(
        @Header("Authorization")
        authorization: String,

        @Query("status")
        status: String? = null,

        @Query("page")
        page: Int = 0,

        @Query("size")
        size: Int = 100
    ): PageResponseDto<RequestResponseDto>

}