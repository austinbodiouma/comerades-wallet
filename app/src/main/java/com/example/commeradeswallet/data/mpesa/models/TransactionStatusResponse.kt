package com.example.commeradeswallet.data.mpesa.models

data class TransactionStatusResponse(
    val ResponseCode: String,
    val ResponseDescription: String,
    val ResultCode: String,
    val ResultDesc: String
) 