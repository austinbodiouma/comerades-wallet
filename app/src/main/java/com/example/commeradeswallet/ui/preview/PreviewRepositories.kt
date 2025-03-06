package com.example.commeradeswallet.ui.preview

import com.example.commeradeswallet.data.dao.OrderDao
import com.example.commeradeswallet.data.dao.WalletDao
import com.example.commeradeswallet.data.model.*
import com.example.commeradeswallet.data.repository.FoodRepository
import com.example.commeradeswallet.data.repository.WalletRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.time.LocalDateTime

// Create mock DAO implementations
object MockWalletDao : WalletDao {
    override fun getTransactions(userId: String): Flow<List<WalletTransaction>> = 
        flowOf(PreviewData.sampleTransactions)
    
    override suspend fun getTransactionByReference(mpesaReference: String): WalletTransaction? = 
        PreviewData.sampleTransactions.firstOrNull { it.reference == mpesaReference }
    
    override suspend fun insertTransaction(transaction: WalletTransaction): Long = 1L
    
    override suspend fun insertTransactions(transactions: List<WalletTransaction>) {}
    
    override suspend fun updateTransactionStatus(reference: String, status: String) {}
}

object MockOrderDao : OrderDao {
    override fun getAllOrders(): Flow<List<Order>> = 
        flowOf(emptyList())
    
    override fun getOrdersByUser(userId: String): Flow<List<Order>> = 
        flowOf(emptyList())
    
    override suspend fun getOrderByCode(orderCode: String): Order? = null
    
    override suspend fun insertOrder(order: Order): Long = 1L
    
    override suspend fun updateOrder(order: Order) {}
    
    override suspend fun deleteOrder(order: Order) {}
}

// Create a factory function to get a WalletRepository with mock DAOs
fun getPreviewWalletRepository(): WalletRepository = 
    WalletRepository(
        walletDao = MockWalletDao,
        orderDao = MockOrderDao,
        firestore = FirebaseFirestore.getInstance()
    )

class PreviewFoodRepository : FoodRepository {
    override fun getAllFoodItems(): Flow<List<FoodItem>> = 
        flowOf(PreviewData.foodItems)
    
    override fun getFoodItemsByCategory(category: String): Flow<List<FoodItem>> = 
        flowOf(PreviewData.foodItems.filter { it.category == category })
    
    override fun searchFoodItems(query: String): Flow<List<FoodItem>> = 
        flowOf(PreviewData.foodItems.filter { it.name.contains(query, ignoreCase = true) })
    
    override suspend fun refreshFoodItems() {}
} 