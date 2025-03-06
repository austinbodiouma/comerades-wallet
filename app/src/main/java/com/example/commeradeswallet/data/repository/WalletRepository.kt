package com.example.commeradeswallet.data.repository

import android.util.Log
import com.example.commeradeswallet.data.dao.OrderDao
import com.example.commeradeswallet.data.dao.WalletDao
import com.example.commeradeswallet.data.model.TransactionType
import com.example.commeradeswallet.data.model.WalletTransaction
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.time.LocalDateTime
import java.time.ZoneId

class WalletRepository(
    private val walletDao: WalletDao,
    private val orderDao: OrderDao,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    fun getWalletBalance(userId: String): Flow<Double> = 
        walletDao.getTransactions(userId)
            .map { transactions -> 
                transactions.sumOf { 
                    if (it.type == TransactionType.DEPOSIT) it.amount else -it.amount 
                }
            }
            .catch { e ->
                Log.e("WalletRepository", "Error getting wallet balance", e)
                emit(0.0)
            }

    fun getTransactions(userId: String): Flow<List<WalletTransaction>> =
        walletDao.getTransactions(userId)
            .catch { e ->
                Log.e("WalletRepository", "Error getting transactions", e)
                emit(emptyList())
            }

    suspend fun processTransaction(
        userId: String,
        amount: Double,
        type: TransactionType,
        description: String,
        reference: String? = null
    ): Result<Unit> = try {
        // First update Firestore
        val result = updateFirestoreWallet(userId, amount, type, description, reference)
        
        if (result.isSuccess) {
            // Then update local database
            val transaction = WalletTransaction(
                userId = userId,
                amount = amount,
                type = type,
                description = description,
                timestamp = LocalDateTime.now(),
                reference = reference
            )
            walletDao.insertTransaction(transaction)
            
            Result.success(Unit)
        } else {
            Result.failure(result.exceptionOrNull() ?: Exception("Unknown error"))
        }
    } catch (e: Exception) {
        Log.e("WalletRepository", "Error processing transaction", e)
        Result.failure(e)
    }

    private suspend fun updateFirestoreWallet(
        userId: String,
        amount: Double,
        type: TransactionType,
        description: String,
        reference: String?
    ): Result<Unit> = try {
        firestore.runTransaction { transaction ->
            val walletRef = firestore.collection("wallets").document(userId)
            val walletDoc = transaction.get(walletRef)
            
            val currentBalance = walletDoc.getDouble("balance") ?: 0.0
            val newBalance = currentBalance + (if (type == TransactionType.WITHDRAWAL) -amount else amount)

            if (type == TransactionType.WITHDRAWAL && newBalance < 0) {
                throw IllegalStateException("Insufficient funds")
            }

            transaction.update(walletRef, "balance", newBalance)

            val transactionData = hashMapOf(
                "userId" to userId,
                "amount" to amount,
                "type" to type.name,
                "description" to description,
                "timestamp" to Timestamp.now(),
                "reference" to reference
            )
            
            transaction.set(
                firestore.collection("transactions").document(),
                transactionData
            )
        }.await()
        
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e("WalletRepository", "Error updating Firestore wallet", e)
        Result.failure(e)
    }

    suspend fun syncWithFirestore(userId: String) {
        try {
            val walletDoc = firestore.collection("wallets")
                .document(userId)
                .get()
                .await()

            val transactions = firestore.collection("transactions")
                .whereEqualTo("userId", userId)
                .get()
                .await()
                .documents
                .mapNotNull { doc ->
                    try {
                        WalletTransaction(
                            userId = doc.getString("userId") ?: userId,
                            amount = doc.getDouble("amount") ?: return@mapNotNull null,
                            type = TransactionType.valueOf(doc.getString("type") ?: return@mapNotNull null),
                            description = doc.getString("description") ?: "",
                            timestamp = (doc.getTimestamp("timestamp")?.toDate()?.toInstant()
                                ?.atZone(ZoneId.systemDefault())
                                ?.toLocalDateTime() ?: LocalDateTime.now()),
                            reference = doc.getString("reference")
                        )
                    } catch (e: Exception) {
                        Log.e("WalletRepository", "Error parsing transaction", e)
                        null
                    }
                }

            walletDao.insertTransactions(transactions)
        } catch (e: Exception) {
            Log.e("WalletRepository", "Error syncing with Firestore", e)
        }
    }
    
    suspend fun getTransactionByReference(reference: String, userId: String): WalletTransaction? {
        return walletDao.getTransactionByReference(reference)
    }
    
    suspend fun updateTransactionStatus(reference: String, status: String) {
        walletDao.updateTransactionStatus(reference, status)
    }
} 