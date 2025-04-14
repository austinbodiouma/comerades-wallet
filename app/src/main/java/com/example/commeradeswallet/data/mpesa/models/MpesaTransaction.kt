package com.example.commeradeswallet.data.mpesa.models

data class MpesaTransaction(
    val userId: String = "",
    val phoneNumber: String = "",
    val amount: Double = 0.0,
    val merchantRequestId: String = "",
    val checkoutRequestId: String = "",
    val status: String = "PENDING",
    val resultCode: String? = null,
    val resultDesc: String? = null,
    val timestamp: Long = System.currentTimeMillis()
) 