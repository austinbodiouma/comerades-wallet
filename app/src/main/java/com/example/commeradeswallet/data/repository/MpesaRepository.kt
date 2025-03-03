package com.example.commeradeswallet.data.repository

import com.example.commeradeswallet.data.mpesa.DarajaClient

class MpesaRepository(
    private val darajaClient: DarajaClient = DarajaClient()
) {
    suspend fun initiateSTKPush(phoneNumber: String, amount: Int): Result<String> {
        return darajaClient.initiateSTKPush(phoneNumber, amount)
    }

    suspend fun checkTransactionStatus(checkoutRequestId: String): Result<Boolean> {
        return darajaClient.checkTransactionStatus(checkoutRequestId)
    }
} 