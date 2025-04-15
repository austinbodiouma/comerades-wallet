package com.example.commeradeswallet.data.mpesa

import com.example.commeradeswallet.data.mpesa.models.STKPushRequest
import com.example.commeradeswallet.data.mpesa.models.STKPushResponse
import com.example.commeradeswallet.data.mpesa.models.STKQueryRequest
import com.example.commeradeswallet.data.mpesa.models.STKQueryResponse
import com.example.commeradeswallet.data.mpesa.models.AccessTokenResponse
import retrofit2.Response
import retrofit2.http.*

interface DarajaClient {
    @Headers("Cache-Control: no-cache")
    @GET("oauth/v1/generate?grant_type=client_credentials")
    suspend fun getAccessToken(
        @Header("Authorization") authorization: String
    ): Response<AccessTokenResponse>

    @Headers("Content-Type: application/json")
    @POST("mpesa/stkpush/v1/processrequest")
    suspend fun stkPush(
        @Body request: STKPushRequest,
        @Header("Authorization") authorization: String
    ): Response<STKPushResponse>

    @Headers("Content-Type: application/json")
    @POST("mpesa/stkpushquery/v1/query")
    suspend fun stkQuery(
        @Body request: STKQueryRequest,
        @Header("Authorization") authorization: String
    ): Response<STKQueryResponse>
} 