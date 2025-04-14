package com.example.commeradeswallet.data.repository

import com.example.commeradeswallet.data.mpesa.DarajaRepository
import com.example.commeradeswallet.data.mpesa.models.STKPushResponse
import com.example.commeradeswallet.data.mpesa.models.STKQueryResponse
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
} 