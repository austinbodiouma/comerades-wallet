package com.example.commeradeswallet.data.mpesa.models

data class STKPushRequest(
    val BusinessShortCode: String,
    val Password: String,
    val Timestamp: String,
    val TransactionType: String = "CustomerPayBillOnline",
    val Amount: Int,
    val PartyA: String,  // Customer phone number
    val PartyB: String,  // Business short code
    val PhoneNumber: String,
    val CallBackURL: String,
    val AccountReference: String,
    val TransactionDesc: String
) 