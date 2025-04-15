package com.example.commeradeswallet.data.repository

import android.util.Log
import com.example.commeradeswallet.data.mpesa.models.MpesaTransaction
import com.example.commeradeswallet.data.mpesa.models.TransactionStatus
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import java.util.*

class MpesaTransactionRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val transactionsCollection = firestore.collection("mpesa_transactions")
    
    suspend fun createTransaction(
        userId: String,
        phoneNumber: String,
        amount: Double,
        merchantRequestId: String,
        checkoutRequestId: String,
        isWalletTopUp: Boolean = false
    ) {
        val transaction = MpesaTransaction(
            userId = userId,
            phoneNumber = phoneNumber,
            amount = amount,
            merchantRequestId = merchantRequestId,
            checkoutRequestId = checkoutRequestId,
            isWalletTopUp = isWalletTopUp
        )
        
        transactionsCollection.document(checkoutRequestId)
            .set(transaction)
            .await()
    }
    
    suspend fun updateTransactionStatus(
        userId: String,
        checkoutRequestId: String,
        status: String,
        resultCode: String?,
        resultDesc: String?
    ) {
        val updates = mapOf(
            "status" to status,
            "resultCode" to resultCode,
            "resultDesc" to resultDesc
        )
        
        transactionsCollection.document(checkoutRequestId)
            .update(updates)
            .await()
    }
    
    fun getTransactionsForUser(userId: String): Flow<List<MpesaTransaction>> = flow {
        val snapshot = transactionsCollection
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .await()
            
        val transactions = snapshot.documents.mapNotNull { doc ->
            doc.toObject(MpesaTransaction::class.java)
        }
        emit(transactions)
    }
    
    suspend fun getTransactionByCheckoutRequestId(checkoutRequestId: String): MpesaTransaction? {
        return transactionsCollection
            .whereEqualTo("checkoutRequestId", checkoutRequestId)
            .get()
            .await()
            .documents
            .firstOrNull()
            ?.toObject(MpesaTransaction::class.java)
    }
} 