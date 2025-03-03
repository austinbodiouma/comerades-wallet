package com.example.commeradeswallet.data.mpesa.models

data class TransactionStatusRequest(
    val BusinessShortCode: String,
    val Password: String,
    val Timestamp: String,
    val CheckoutRequestID: String
)

data class TransactionStatusResponse(
    val ResponseCode: String,
    val ResponseDescription: String,
    val ResultCode: String,
    val ResultDesc: String
) 