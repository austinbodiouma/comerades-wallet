package com.example.commeradeswallet.data.mpesa

import android.util.Base64
import android.util.Log
import com.example.commeradeswallet.data.mpesa.api.DarajaApiService
import com.example.commeradeswallet.data.mpesa.models.STKPushRequest
import com.example.commeradeswallet.data.mpesa.models.STKQueryRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import okhttp3.Credentials

/**
 * Daraja Client for interacting with the M-PESA API
 */
class DarajaClient {
    private val TAG = "DarajaClient"
    
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

    /**
     * Get timestamp in the format required by M-Pesa API
     */
    private fun getTimestamp(): String {
        val sdf = SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault())
        return sdf.format(Date())
    }

    /**
     * Generate password for M-Pesa API authentication
     */
    private fun generatePassword(timestamp: String): String {
        val password = "${DarajaCredentials.BUSINESS_SHORT_CODE}${DarajaCredentials.PASSKEY}$timestamp"
        return Base64.encodeToString(password.toByteArray(), Base64.NO_WRAP)
    }

    /**
     * Get OAuth token for API authentication
     */
    private suspend fun getAuthToken(): String? {
        try {
            val credentials = Credentials.basic(
                DarajaCredentials.CONSUMER_KEY,
                DarajaCredentials.CONSUMER_SECRET
            )
            
            val response = apiService.getAccessToken(credentials)
            
            if (response.isSuccessful && response.body() != null) {
                return response.body()!!.access_token
            } else {
                Log.e(TAG, "Failed to get auth token: ${response.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting auth token", e)
        }
        
        return null
    }

    /**
     * Initialize STK Push request to M-PESA
     * 
     * @param phoneNumber Phone number to prompt for payment
     * @param amount Amount to charge
     * @param reference Optional reference for the transaction
     * @return Result containing CheckoutRequestID or error
     */
    suspend fun initiateSTKPush(
        phoneNumber: String,
        amount: Int,
        reference: String = ""
    ): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Initiating STK push for $phoneNumber, amount: $amount, reference: $reference")
                
                // Format the phone number if needed
                val formattedPhone = formatPhoneNumber(phoneNumber)
                    ?: return@withContext Result.failure(Exception("Invalid phone number format"))
                
                // Get OAuth token
                val authToken = getAuthToken()
                    ?: return@withContext Result.failure(Exception("Failed to get authentication token"))
                
                Log.d(TAG, "Auth token obtained: $authToken")
                
                // Generate timestamp
                val timestamp = getTimestamp()
                
                // Create STK Push request
                val stkRequest = STKPushRequest(
                    BusinessShortCode = DarajaCredentials.BUSINESS_SHORT_CODE,
                    Password = generatePassword(timestamp),
                    Timestamp = timestamp,
                    Amount = amount,
                    PartyA = formattedPhone,
                    PartyB = DarajaCredentials.BUSINESS_SHORT_CODE,
                    PhoneNumber = formattedPhone,
                    CallBackURL = DarajaCredentials.CALLBACK_URL,
                    AccountReference = reference.ifEmpty { "CommeradesWallet" },
                    TransactionDesc = "Wallet Top-up"
                )
                
                // Execute request
                val response = apiService.performStkPush("Bearer $authToken", stkRequest)
                
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    Log.d(TAG, "STK Push Response: $body")
                    
                    if (body.ResponseCode == "0") {
                        Result.success(body.CheckoutRequestID)
                    } else {
                        Result.failure(Exception(body.ResponseDescription))
                    }
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Unknown error"
                    Log.e(TAG, "STK Push failed: $errorBody")
                    Result.failure(Exception("Failed to initiate STK push: $errorBody"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in STK push", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Formats the phone number to the required format (254XXXXXXXXX)
     */
    private fun formatPhoneNumber(phone: String): String? {
        return when {
            // Already in correct format
            phone.matches(Regex("^254\\d{9}$")) -> phone
            
            // Format from 07XXXXXXXX to 254XXXXXXXX
            phone.matches(Regex("^0\\d{9}$")) -> "254${phone.substring(1)}"
            
            // Format from +254XXXXXXXX to 254XXXXXXXX
            phone.matches(Regex("^\\+254\\d{9}$")) -> phone.substring(1)
            
            // Invalid format
            else -> null
        }
    }

    suspend fun checkTransactionStatus(checkoutRequestId: String): Result<Boolean> = 
        withContext(Dispatchers.IO) {
            try {
                // Get access token
                val accessToken = getAuthToken()
                    ?: return@withContext Result.failure(Exception("Failed to get access token"))

                // Prepare status request
                val timestamp = getTimestamp()
                val statusRequest = STKQueryRequest(
                    BusinessShortCode = DarajaCredentials.BUSINESS_SHORT_CODE,
                    Password = generatePassword(timestamp),
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