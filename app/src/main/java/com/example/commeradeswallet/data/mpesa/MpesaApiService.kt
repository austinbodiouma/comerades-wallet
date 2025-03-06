package com.example.commeradeswallet.data.mpesa

import com.example.commeradeswallet.data.mpesa.models.AccessTokenResponse
import com.example.commeradeswallet.data.mpesa.models.STKPushRequest
import com.example.commeradeswallet.data.mpesa.models.STKPushResponse
import com.example.commeradeswallet.data.mpesa.models.STKQueryRequest
import com.example.commeradeswallet.data.mpesa.models.STKQueryResponse
import retrofit2.Response
import retrofit2.http.*

interface MpesaApiService {
    @Headers("Content-Type: application/json")
    @POST("mpesa/stkpush/v1/processrequest")
    suspend fun initiateSTKPush(
        @Header("Authorization") token: String,
        @Body request: STKPushRequest
    ): Response<STKPushResponse>

    @GET("oauth/v1/generate?grant_type=client_credentials")
    suspend fun getAccessToken(
        @Header("Authorization") credentials: String
    ): Response<AccessTokenResponse>

    @Headers("Content-Type: application/json")
    @POST("mpesa/stkpushquery/v1/query")
    suspend fun queryTransactionStatus(
        @Header("Authorization") token: String,
        @Body request: STKQueryRequest
    ): Response<STKQueryResponse>
}

data class AccessTokenResponse(
    val access_token: String,
    val expires_in: String
)

data class STKPushRequest(
    val BusinessShortCode: String,
    val Password: String,
    val Timestamp: String,
    val TransactionType: String = "CustomerPayBillOnline",
    val Amount: Int,
    val PartyA: String,  // Customer phone number
    val PartyB: String,  // Business short code
    val PhoneNumber: String,
    val CallBackURL: String,
    val AccountReference: String,
    val TransactionDesc: String
)

data class STKPushResponse(
    val MerchantRequestID: String,
    val CheckoutRequestID: String,
    val ResponseCode: String,
    val ResponseDescription: String,
    val CustomerMessage: String
)

data class STKQueryRequest(
    val BusinessShortCode: String,
    val Password: String,
    val Timestamp: String,
    val CheckoutRequestID: String
)

data class STKQueryResponse(
    val ResponseCode: String,
    val ResponseDescription: String,
    val MerchantRequestID: String,
    val CheckoutRequestID: String,
    val ResultCode: String,
    val ResultDesc: String
) 