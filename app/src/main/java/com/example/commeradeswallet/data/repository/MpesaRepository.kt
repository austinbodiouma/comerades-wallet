package com.example.commeradeswallet.data.repository

import android.util.Base64
import android.util.Log
import com.example.commeradeswallet.data.mpesa.DarajaCredentials
import com.example.commeradeswallet.data.mpesa.MpesaApiService
import com.example.commeradeswallet.data.mpesa.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.*

class MpesaRepository {
    private val api: MpesaApiService by lazy {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .build()

        Retrofit.Builder()
            .baseUrl(DarajaCredentials.BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MpesaApiService::class.java)
    }

    suspend fun initiateSTKPush(phoneNumber: String, amount: Int, reference: String = ""): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("MpesaRepository", "Initiating STK push for phone: $phoneNumber, amount: $amount, reference: $reference")
                
                val timestamp = SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(Date())
                val password = generatePassword(timestamp)

                val request = STKPushRequest(
                    BusinessShortCode = DarajaCredentials.BUSINESS_SHORT_CODE,
                    Password = password,
                    Timestamp = timestamp,
                    Amount = amount,
                    PartyA = phoneNumber,
                    PartyB = DarajaCredentials.BUSINESS_SHORT_CODE,
                    PhoneNumber = phoneNumber,
                    CallBackURL = DarajaCredentials.CALLBACK_URL,
                    AccountReference = reference.ifEmpty { "Commerades Wallet" },
                    TransactionDesc = "Wallet Top-up"
                )

                val authToken = getAuthToken()
                Log.d("MpesaRepository", "Got auth token: $authToken")

                val response = api.initiateSTKPush("Bearer $authToken", request)
                Log.d("MpesaRepository", "STK push response: ${response.body()}")

                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    if (body.ResponseCode == "0") {
                        Log.d("MpesaRepository", "STK push successful with CheckoutRequestID: ${body.CheckoutRequestID}")
                        Result.success(body.CheckoutRequestID)
                    } else {
                        Log.e("MpesaRepository", "STK push failed with response: ${body.ResponseDescription}")
                        Result.failure(Exception(body.ResponseDescription))
                    }
                } else {
                    Log.e("MpesaRepository", "STK push failed with error: ${response.errorBody()?.string()}")
                    Result.failure(Exception("Failed to initiate payment"))
                }
            } catch (e: Exception) {
                Log.e("MpesaRepository", "Error initiating STK push", e)
                Result.failure(e)
            }
        }
    }

    private suspend fun getAuthToken(): String {
        val auth = Credentials.basic(DarajaCredentials.CONSUMER_KEY, DarajaCredentials.CONSUMER_SECRET)
        val response = api.getAccessToken("Basic $auth")
        
        if (response.isSuccessful && response.body() != null) {
            return response.body()!!.access_token
        } else {
            throw Exception("Failed to get auth token")
        }
    }

    private fun generatePassword(timestamp: String): String {
        val str = "${DarajaCredentials.BUSINESS_SHORT_CODE}${DarajaCredentials.PASSKEY}$timestamp"
        return Base64.encodeToString(str.toByteArray(), Base64.NO_WRAP)
    }

    suspend fun checkTransactionStatus(checkoutRequestId: String): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val timestamp = SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(Date())
                val password = generatePassword(timestamp)

                val request = STKQueryRequest(
                    BusinessShortCode = DarajaCredentials.BUSINESS_SHORT_CODE,
                    Password = password,
                    Timestamp = timestamp,
                    CheckoutRequestID = checkoutRequestId
                )

                val authToken = getAuthToken()
                val response = api.queryTransactionStatus("Bearer $authToken", request)

                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    Result.success(body.ResultCode == "0")
                } else {
                    Result.failure(Exception("Failed to check transaction status"))
                }
            } catch (e: Exception) {
                Log.e("MpesaRepository", "Error checking transaction status", e)
                Result.failure(e)
            }
        }
    }
} 