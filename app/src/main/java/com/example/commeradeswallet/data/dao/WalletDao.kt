package com.example.commeradeswallet.data.dao

import androidx.room.*
import com.example.commeradeswallet.data.model.WalletTransaction
import kotlinx.coroutines.flow.Flow

@Dao
interface WalletDao {
    @Query("SELECT * FROM wallet_transactions WHERE userId = :userId ORDER BY timestamp DESC")
    fun getTransactions(userId: String): Flow<List<WalletTransaction>>

    @Query("SELECT * FROM wallet_transactions WHERE reference = :mpesaReference")
    suspend fun getTransactionByReference(mpesaReference: String): WalletTransaction?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: WalletTransaction): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<WalletTransaction>)

    @Query("UPDATE wallet_transactions SET status = :status WHERE reference = :reference")
    suspend fun updateTransactionStatus(reference: String, status: String)
} 