package com.example.commeradeswallet.data.repository

import com.example.commeradeswallet.data.mpesa.DarajaRepository
import com.example.commeradeswallet.data.mpesa.models.STKPushResponse
import com.example.commeradeswallet.data.mpesa.models.STKQueryResponse
import com.example.commeradeswallet.utils.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MpesaRepository(
    private val darajaRepository: DarajaRepository
) {
    suspend fun initiateSTKPush(
        phoneNumber: String,
        amount: Int,
        accountReference: String = "CompanyXLTD",
        transactionDesc: String = "Payment of X"
    ): Result<STKPushResponse> {
        return darajaRepository.initiateSTKPush(
            phoneNumber = phoneNumber,
            amount = amount,
            accountReference = accountReference,
            transactionDesc = transactionDesc
        )
    }

    suspend fun querySTKStatus(checkoutRequestId: String): Result<STKQueryResponse> {
        return darajaRepository.querySTKStatus(checkoutRequestId)
    }
    
    suspend fun queryTransactionStatus(checkoutRequestId: String): Resource<STKQueryResponse> {
        return try {
            val result = darajaRepository.querySTKStatus(checkoutRequestId)
            if (result.isSuccess) {
                Resource.Success(result.getOrNull())
            } else {
                Resource.Error(result.exceptionOrNull()?.message ?: "Unknown error")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Unknown error")
        }
    }
    
    suspend fun initiateStk(phoneNumber: String, amount: String, paymentReason: String): Resource<STKPushResponse> {
        return try {
            val amountInt = amount.toInt()
            val result = darajaRepository.initiateSTKPush(
                phoneNumber = phoneNumber,
                amount = amountInt,
                accountReference = paymentReason,
                transactionDesc = if (paymentReason == "WALLET_TOPUP") "Wallet Top Up" else paymentReason
            )
            
            if (result.isSuccess) {
                Resource.Success(result.getOrNull())
            } else {
                Resource.Error(result.exceptionOrNull()?.message ?: "Failed to initiate transaction")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Unknown error")
        }
    }
} 