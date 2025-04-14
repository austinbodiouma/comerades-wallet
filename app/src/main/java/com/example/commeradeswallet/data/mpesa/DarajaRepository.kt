package com.example.commeradeswallet.data.mpesa

import android.util.Base64
import android.util.Log
import com.example.commeradeswallet.data.mpesa.models.STKPushRequest
import com.example.commeradeswallet.data.mpesa.models.STKPushResponse
import com.example.commeradeswallet.data.mpesa.models.STKQueryRequest
import com.example.commeradeswallet.data.mpesa.models.STKQueryResponse
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class DarajaRepository(
    private val client: DarajaClient,
    private val consumerKey: String,
    private val consumerSecret: String,
    private val passKey: String,
    private val businessShortCode: String,
    private val callbackUrl: String
) {
    private var accessToken: String? = null
    private var tokenExpiry: Long = 0
    private val mutex = Mutex()
    private val TAG = "DarajaRepository"

    private fun generateTimestamp(): String {
        val format = SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault())
        return format.format(Date())
    }

    private fun generatePassword(timestamp: String): String {
        val passwordString = "$businessShortCode$passKey$timestamp"
        Log.d(TAG, "Generating password with: shortcode=$businessShortCode, timestamp=$timestamp")
        val password = Base64.encodeToString(passwordString.toByteArray(), Base64.NO_WRAP)
        Log.d(TAG, "Generated password: $password")
        return password
    }

    private fun generateAuthorizationHeader(): String {
        val credentials = "$consumerKey:$consumerSecret"
        Log.d(TAG, "Generating auth header with credentials length: ${credentials.length}")
        val encodedCredentials = Base64.encodeToString(credentials.toByteArray(), Base64.NO_WRAP)
        return "Basic $encodedCredentials"
    }

    private suspend fun getAccessToken(): String {
        return mutex.withLock {
            val currentTime = System.currentTimeMillis()
            if (accessToken != null && currentTime < tokenExpiry) {
                Log.d(TAG, "Using cached access token")
                return@withLock accessToken!!
            }

            try {
                Log.d(TAG, "Fetching new access token")
                val authHeader = generateAuthorizationHeader()
                Log.d(TAG, "Auth header generated: ${authHeader.take(20)}...")
                
                val response = client.getAccessToken(authorization = authHeader)
                Log.d(TAG, "Access token response: isSuccessful=${response.isSuccessful}, code=${response.code()}")

                if (!response.isSuccessful) {
                    Log.e(TAG, "Access token error response: ${response.errorBody()?.string()}")
                    throw Exception("Failed to get access token: ${response.code()} ${response.message()}")
                }

                if (response.body() == null) {
                    Log.e(TAG, "Access token response body is null")
                    throw Exception("Access token response body is null")
                }

                val tokenResponse = response.body()!!
                accessToken = tokenResponse.accessToken
                tokenExpiry = currentTime + (tokenResponse.expiresIn.toLong() * 1000) - 60000
                Log.d(TAG, "New access token obtained, expires in: ${tokenResponse.expiresIn}s")
                return@withLock accessToken!!
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get access token", e)
                throw e
            }
        }
    }

    suspend fun initiateSTKPush(
        phoneNumber: String,
        amount: Int,
        accountReference: String = "CompanyXLTD",
        transactionDesc: String = "Payment of X"
    ): Result<STKPushResponse> {
        return try {
            Log.d(TAG, "Initiating STK Push for phone: $phoneNumber, amount: $amount")
            val timestamp = generateTimestamp()
            val password = generatePassword(timestamp)
            val token = getAccessToken()
            Log.d(TAG, "Got access token: ${token.take(10)}...")

            // Format phone number to include country code if not present
            val formattedPhone = if (phoneNumber.startsWith("254")) {
                phoneNumber
            } else if (phoneNumber.startsWith("0")) {
                "254${phoneNumber.substring(1)}"
            } else if (phoneNumber.startsWith("+254")) {
                phoneNumber.substring(1)
            } else {
                phoneNumber
            }
            Log.d(TAG, "Formatted phone number: $formattedPhone")

            val request = STKPushRequest(
                businessShortCode = businessShortCode,
                password = password,
                timestamp = timestamp,
                transactionType = "CustomerPayBillOnline",
                amount = amount,
                partyA = formattedPhone,
                partyB = businessShortCode,
                phoneNumber = formattedPhone,
                callBackUrl = callbackUrl,
                accountReference = accountReference,
                transactionDesc = transactionDesc
            )
            Log.d(TAG, "STK Push request created: $request")

            val response = client.stkPush(
                request = request,
                authorization = "Bearer $token"
            )
            Log.d(TAG, "STK Push response: isSuccessful=${response.isSuccessful}, code=${response.code()}")

            if (!response.isSuccessful) {
                val errorBody = response.errorBody()?.string()
                Log.e(TAG, "STK Push error response: $errorBody")
                return Result.failure(Exception("STK Push failed: ${response.code()} ${response.message()}\n$errorBody"))
            }

            if (response.body() == null) {
                Log.e(TAG, "STK Push response body is null")
                return Result.failure(Exception("STK Push response body is null"))
            }

            val stkResponse = response.body()!!
            Log.d(TAG, "STK Push successful: $stkResponse")
            Result.success(stkResponse)
        } catch (e: Exception) {
            Log.e(TAG, "STK Push failed", e)
            Result.failure(e)
        }
    }

    suspend fun querySTKStatus(checkoutRequestId: String): Result<STKQueryResponse> {
        return try {
            Log.d(TAG, "Querying STK status for checkoutRequestId: $checkoutRequestId")
            val timestamp = generateTimestamp()
            val password = generatePassword(timestamp)
            val token = getAccessToken()
            Log.d(TAG, "Got access token for query: ${token.take(10)}...")

            val request = STKQueryRequest(
                businessShortCode = businessShortCode,
                password = password,
                timestamp = timestamp,
                checkoutRequestID = checkoutRequestId
            )
            Log.d(TAG, "STK Query request created: $request")

            val response = client.stkQuery(
                request = request,
                authorization = "Bearer $token"
            )
            Log.d(TAG, "STK Query response: isSuccessful=${response.isSuccessful}, code=${response.code()}")

            if (!response.isSuccessful) {
                val errorBody = response.errorBody()?.string()
                Log.e(TAG, "STK Query error response: $errorBody")
                return Result.failure(Exception("STK Query failed: ${response.code()} ${response.message()}\n$errorBody"))
            }

            if (response.body() == null) {
                Log.e(TAG, "STK Query response body is null")
                return Result.failure(Exception("STK Query response body is null"))
            }

            val queryResponse = response.body()!!
            Log.d(TAG, "STK Query successful: $queryResponse")
            Result.success(queryResponse)
        } catch (e: Exception) {
            Log.e(TAG, "STK Query failed", e)
            Result.failure(e)
        }
    }
} 