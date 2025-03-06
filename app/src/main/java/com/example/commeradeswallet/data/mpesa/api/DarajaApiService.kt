package com.example.commeradeswallet.data.mpesa.api

import com.example.commeradeswallet.data.mpesa.models.AccessTokenResponse
import com.example.commeradeswallet.data.mpesa.models.STKPushRequest
import com.example.commeradeswallet.data.mpesa.models.STKPushResponse
import com.example.commeradeswallet.data.mpesa.models.STKQueryRequest
import com.example.commeradeswallet.data.mpesa.models.STKQueryResponse
import retrofit2.Response
import retrofit2.http.*

interface DarajaApiService {
    @Headers("Content-Type: application/json")
    @POST("mpesa/stkpush/v1/processrequest")
    suspend fun performStkPush(
        @Header("Authorization") token: String,
        @Body request: STKPushRequest
    ): Response<STKPushResponse>

    @GET("oauth/v1/generate?grant_type=client_credentials")
    suspend fun getAccessToken(
        @Header("Authorization") credentials: String
    ): Response<AccessTokenResponse>

    @Headers("Content-Type: application/json")
    @POST("mpesa/stkpushquery/v1/query")
    suspend fun checkTransactionStatus(
        @Header("Authorization") token: String,
        @Body request: STKQueryRequest
    ): Response<STKQueryResponse>
} 