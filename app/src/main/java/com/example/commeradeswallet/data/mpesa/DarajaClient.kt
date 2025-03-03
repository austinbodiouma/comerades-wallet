package com.example.commeradeswallet.data.mpesa

import android.util.Base64
import com.example.commeradeswallet.data.mpesa.api.DarajaApiService
import com.example.commeradeswallet.data.mpesa.models.STKPushRequest
import com.example.commeradeswallet.data.mpesa.models.TransactionStatusRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class DarajaClient {
    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(DarajaCredentials.BASE_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private val apiService: DarajaApiService by lazy {
        retrofit.create(DarajaApiService::class.java)
    }

    private fun getAuthToken(): String {
        val credentials = "${DarajaCredentials.CONSUMER_KEY}:${DarajaCredentials.CONSUMER_SECRET}"
        return "Basic " + Base64.encodeToString(credentials.toByteArray(), Base64.NO_WRAP)
    }

    private fun getTimestamp(): String {
        val sdf = SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault())
        return sdf.format(Date())
    }

    private fun generatePassword(): String {
        val timestamp = getTimestamp()
        val password = "${DarajaCredentials.BUSINESS_SHORT_CODE}${DarajaCredentials.PASSKEY}$timestamp"
        return Base64.encodeToString(password.toByteArray(), Base64.NO_WRAP)
    }

    suspend fun initiateSTKPush(
        phoneNumber: String,
        amount: Int
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Get access token
            val tokenResponse = apiService.getAccessToken(getAuthToken())
            if (!tokenResponse.isSuccessful) {
                return@withContext Result.failure(Exception("Failed to get access token"))
            }

            val accessToken = tokenResponse.body()?.access_token
                ?: return@withContext Result.failure(Exception("Access token is null"))

            // Prepare STK Push request
            val timestamp = getTimestamp()
            val stkRequest = STKPushRequest(
                BusinessShortCode = DarajaCredentials.BUSINESS_SHORT_CODE,
                Password = generatePassword(),
                Timestamp = timestamp,
                Amount = amount,
                PartyA = phoneNumber,
                PartyB = DarajaCredentials.BUSINESS_SHORT_CODE,
                PhoneNumber = phoneNumber,
                CallBackURL = DarajaCredentials.CALLBACK_URL
            )

            // Perform STK Push
            val stkResponse = apiService.performStkPush(
                "Bearer $accessToken",
                stkRequest
            )

            if (stkResponse.isSuccessful) {
                val responseBody = stkResponse.body()
                if (responseBody?.ResponseCode == "0") {
                    Result.success(responseBody.CheckoutRequestID)
                } else {
                    Result.failure(Exception(responseBody?.CustomerMessage ?: "STK Push failed"))
                }
            } else {
                Result.failure(Exception("STK Push request failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun checkTransactionStatus(checkoutRequestId: String): Result<Boolean> = 
        withContext(Dispatchers.IO) {
            try {
                // Get access token
                val tokenResponse = apiService.getAccessToken(getAuthToken())
                if (!tokenResponse.isSuccessful) {
                    return@withContext Result.failure(Exception("Failed to get access token"))
                }

                val accessToken = tokenResponse.body()?.access_token
                    ?: return@withContext Result.failure(Exception("Access token is null"))

                // Prepare status request
                val timestamp = getTimestamp()
                val statusRequest = TransactionStatusRequest(
                    BusinessShortCode = DarajaCredentials.BUSINESS_SHORT_CODE,
                    Password = generatePassword(),
                    Timestamp = timestamp,
                    CheckoutRequestID = checkoutRequestId
                )

                // Check status
                val statusResponse = apiService.checkTransactionStatus(
                    "Bearer $accessToken",
                    statusRequest
                )

                if (statusResponse.isSuccessful) {
                    val responseBody = statusResponse.body()
                    when (responseBody?.ResultCode) {
                        "0" -> Result.success(true)  // Transaction successful
                        "1037" -> Result.success(false) // Timeout
                        "1032" -> Result.success(false) // Cancelled
                        else -> Result.failure(
                            Exception(responseBody?.ResultDesc ?: "Unknown status")
                        )
                    }
                } else {
                    Result.failure(Exception("Status check failed"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
} 