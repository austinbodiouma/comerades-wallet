package com.example.commeradeswallet.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.commeradeswallet.data.model.WalletTransaction
import com.example.commeradeswallet.data.model.TransactionStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface WalletDao {
    @Query("SELECT SUM(CASE WHEN type = 'DEPOSIT' THEN amount ELSE -amount END) FROM wallet_transactions")
    fun getBalance(): Flow<Double>

    @Query("SELECT SUM(CASE WHEN type = 'DEPOSIT' THEN amount ELSE -amount END) FROM wallet_transactions")
    suspend fun getCurrentBalance(): Double

    @Query("SELECT * FROM wallet_transactions ORDER BY timestamp DESC")
    fun getTransactions(): Flow<List<WalletTransaction>>

    @Query("SELECT * FROM wallet_transactions WHERE reference = :mpesaReference")
    suspend fun getTransactionByReference(mpesaReference: String): WalletTransaction?

    @Insert
    suspend fun insertTransaction(transaction: WalletTransaction): Long

    @Update
    suspend fun updateTransaction(transaction: WalletTransaction)

    @Query("UPDATE wallet_transactions SET status = :status WHERE reference = :reference")
    suspend fun updateTransactionStatus(reference: String, status: TransactionStatus)
} 