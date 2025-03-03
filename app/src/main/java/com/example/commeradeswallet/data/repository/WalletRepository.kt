package com.example.commeradeswallet.data.repository

import com.example.commeradeswallet.data.dao.OrderDao
import com.example.commeradeswallet.data.dao.WalletDao
import com.example.commeradeswallet.data.model.Order
import com.example.commeradeswallet.data.model.TransactionType
import com.example.commeradeswallet.data.model.WalletTransaction
import kotlinx.coroutines.flow.Flow
import java.util.UUID

open class WalletRepository(
    private val walletDao: WalletDao,
    private val orderDao: OrderDao
) {
    fun getBalance(): Flow<Double> = walletDao.getBalance()
    
    fun getTransactions(): Flow<List<WalletTransaction>> = 
        walletDao.getTransactions()

    suspend fun processTopUp(amount: Double, mpesaReference: String) {
        walletDao.insertTransaction(
            WalletTransaction(
                amount = amount,
                type = TransactionType.DEPOSIT,
                description = "M-Pesa Top-up",
                reference = mpesaReference
            )
        )
    }

    suspend fun processOrder(order: Order): Result<String> {
        val balance = walletDao.getCurrentBalance()
        if (balance < order.totalAmount) {
            return Result.failure(Exception("Insufficient balance"))
        }

        // Generate unique order code
        val orderCode = generateOrderCode()
        
        // Create withdrawal transaction
        walletDao.insertTransaction(
            WalletTransaction(
                amount = -order.totalAmount,
                type = TransactionType.WITHDRAWAL,
                description = "Food Order #$orderCode"
            )
        )

        // Save order
        orderDao.insertOrder(order.copy(orderCode = orderCode))

        return Result.success(orderCode)
    }

    private fun generateOrderCode(): String {
        return UUID.randomUUID().toString().take(8).uppercase()
    }
} 