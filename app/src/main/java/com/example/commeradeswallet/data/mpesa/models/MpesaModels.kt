package com.example.commeradeswallet.data.mpesa.models

data class AccessTokenResponse(
    val access_token: String,
    val expires_in: String
)

data class STKPushRequest(
    val BusinessShortCode: String,
    val Password: String,
    val Timestamp: String,
    val TransactionType: String = "CustomerPayBillOnline",
    val Amount: Int,
    val PartyA: String, // Customer phone number
    val PartyB: String, // Business short code
    val PhoneNumber: String, // Customer phone number
    val CallBackURL: String,
    val AccountReference: String = "CommeradesWallet",
    val TransactionDesc: String = "Wallet Top-up"
)

data class STKPushResponse(
    val MerchantRequestID: String,
    val CheckoutRequestID: String,
    val ResponseCode: String,
    val ResponseDescription: String,
    val CustomerMessage: String
) 