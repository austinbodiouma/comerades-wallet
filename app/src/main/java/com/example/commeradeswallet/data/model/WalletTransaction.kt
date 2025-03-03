package com.example.commeradeswallet.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "wallet_transactions")
data class WalletTransaction(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val amount: Double,
    val type: TransactionType,
    val description: String,
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val reference: String? = null, // For M-Pesa transaction reference
    val status: TransactionStatus = TransactionStatus.PENDING
)

enum class TransactionType {
    DEPOSIT, // M-Pesa top-up
    WITHDRAWAL, // Food purchase
    REFUND // In case of cancelled orders
}

enum class TransactionStatus {
    PENDING,
    COMPLETED,
    FAILED
} 