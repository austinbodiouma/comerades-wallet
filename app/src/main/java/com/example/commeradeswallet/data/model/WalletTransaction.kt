package com.example.commeradeswallet.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "wallet_transactions")
data class WalletTransaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String,
    val amount: Double,
    val type: TransactionType,
    val description: String,
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val reference: String? = null,
    val status: String = "PENDING"
)

enum class TransactionType {
    DEPOSIT,
    WITHDRAWAL
}

enum class TransactionStatus {
    PENDING,
    COMPLETED,
    FAILED
} 