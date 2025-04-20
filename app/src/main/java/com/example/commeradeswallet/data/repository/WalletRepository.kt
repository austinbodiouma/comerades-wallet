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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow

class WalletRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val walletDao: WalletDao? = null,
    private val orderDao: OrderDao? = null
) {
    private val walletsCollection = firestore.collection("wallets")

    fun getWalletBalance(userId: String): Flow<Double> = callbackFlow {
        val listener = walletsCollection.document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val balance = snapshot?.getDouble("balance") ?: 0.0
                trySend(balance)
            }
            
        awaitClose { listener.remove() }
    }

    fun getTransactions(userId: String): Flow<List<WalletTransaction>> =
        walletDao?.getTransactions(userId)
            ?.catch { e ->
                Log.e("WalletRepository", "Error getting transactions", e)
                emit(emptyList())
            }
            ?: callbackFlow { close(IllegalStateException("WalletDao is null")) }

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
            walletDao?.insertTransaction(transaction)
            
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

            walletDao?.insertTransactions(transactions)
        } catch (e: Exception) {
            Log.e("WalletRepository", "Error syncing with Firestore", e)
        }
    }
    
    suspend fun getTransactionByReference(reference: String, userId: String): WalletTransaction? {
        return walletDao?.getTransactionByReference(reference)
    }
    
    suspend fun updateTransactionStatus(reference: String, status: String) {
        walletDao?.updateTransactionStatus(reference, status)
    }

    suspend fun updateBalance(update: (Double) -> Double) {
        val userId = auth.currentUser?.uid ?: throw IllegalStateException("User not logged in")
        val docRef = walletsCollection.document(userId)
        
        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(docRef)
            val currentBalance = snapshot.getDouble("balance") ?: 0.0
            val newBalance = update(currentBalance)
            
            transaction.set(docRef, mapOf(
                "balance" to newBalance,
                "updatedAt" to com.google.firebase.Timestamp.now()
            ), com.google.firebase.firestore.SetOptions.merge())
        }.await()
    }

    suspend fun createWalletIfNotExists() {
        val userId = auth.currentUser?.uid ?: throw IllegalStateException("User not logged in")
        val docRef = walletsCollection.document(userId)
        
        val snapshot = docRef.get().await()
        if (!snapshot.exists()) {
            docRef.set(mapOf(
                "balance" to 0.0,
                "createdAt" to com.google.firebase.Timestamp.now(),
                "updatedAt" to com.google.firebase.Timestamp.now()
            )).await()
        }
    }
    
    suspend fun addFunds(amount: Double) {
        val userId = auth.currentUser?.uid ?: throw IllegalStateException("User not logged in")
        
        try {
            // Process the transaction to add funds to the wallet
            processTransaction(
                userId = userId,
                amount = amount,
                type = TransactionType.DEPOSIT,
                description = "Wallet top-up"
            )
            
            Log.d("WalletRepository", "Successfully added $amount to wallet")
        } catch (e: Exception) {
            Log.e("WalletRepository", "Error adding funds to wallet", e)
            throw e
        }
    }
} 