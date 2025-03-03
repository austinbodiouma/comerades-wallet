package com.example.commeradeswallet.ui.preview

import com.example.commeradeswallet.data.dao.OrderDao
import com.example.commeradeswallet.data.dao.WalletDao
import com.example.commeradeswallet.data.model.Order
import com.example.commeradeswallet.data.model.WalletTransaction
import com.example.commeradeswallet.data.model.TransactionStatus
import com.example.commeradeswallet.data.repository.WalletRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class PreviewWalletRepository : WalletRepository(
    walletDao = object : WalletDao {
        override fun getBalance(): Flow<Double> = flowOf(1500.0)
        override suspend fun getCurrentBalance(): Double = 1500.0
        override fun getTransactions(): Flow<List<WalletTransaction>> = 
            flowOf(PreviewData.sampleTransactions)
        override suspend fun getTransactionByReference(mpesaReference: String): WalletTransaction? = null
        override suspend fun insertTransaction(transaction: WalletTransaction): Long = 0L
        override suspend fun updateTransaction(transaction: WalletTransaction) {}
        override suspend fun updateTransactionStatus(reference: String, status: TransactionStatus) {}
    },
    orderDao = object : OrderDao {
        override fun getAllOrders(): Flow<List<Order>> = flowOf(emptyList())
        override suspend fun getOrderByCode(code: String): Order? = null
        override suspend fun insertOrder(order: Order) {}
        override suspend fun updateOrder(order: Order) {}
    }
) 