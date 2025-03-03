package com.example.commeradeswallet.data.mpesa.api

import com.example.commeradeswallet.data.mpesa.models.AccessTokenResponse
import com.example.commeradeswallet.data.mpesa.models.STKPushRequest
import com.example.commeradeswallet.data.mpesa.models.STKPushResponse
import com.example.commeradeswallet.data.mpesa.models.TransactionStatusRequest
import com.example.commeradeswallet.data.mpesa.models.TransactionStatusResponse
import retrofit2.Response
import retrofit2.http.*

interface DarajaApiService {
    @GET("oauth/v1/generate?grant_type=client_credentials")
    suspend fun getAccessToken(
        @Header("Authorization") credentials: String
    ): Response<AccessTokenResponse>

    @POST("mpesa/stkpush/v1/processrequest")
    suspend fun performStkPush(
        @Header("Authorization") token: String,
        @Body stkPushRequest: STKPushRequest
    ): Response<STKPushResponse>

    @POST("mpesa/stkpushquery/v1/query")
    suspend fun checkTransactionStatus(
        @Header("Authorization") token: String,
        @Body statusRequest: TransactionStatusRequest
    ): Response<TransactionStatusResponse>
} 