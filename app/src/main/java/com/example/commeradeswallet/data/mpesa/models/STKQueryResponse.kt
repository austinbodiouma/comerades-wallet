package com.example.commeradeswallet.data.mpesa.models

data class STKQueryResponse(
    val ResponseCode: String,
    val ResponseDescription: String,
    val MerchantRequestID: String,
    val CheckoutRequestID: String,
    val ResultCode: String,
    val ResultDesc: String
) 