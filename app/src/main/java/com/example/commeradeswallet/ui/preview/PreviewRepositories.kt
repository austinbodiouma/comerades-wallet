package com.example.commeradeswallet.ui.preview

import com.example.commeradeswallet.data.dao.OrderDao
import com.example.commeradeswallet.data.dao.WalletDao
import com.example.commeradeswallet.data.model.*
import com.example.commeradeswallet.data.repository.FoodRepository
import com.example.commeradeswallet.data.repository.WalletRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
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
    private val orders = mutableListOf<Order>()

    override fun getAllOrders(): Flow<List<Order>> = flow { emit(orders) }

    override fun getOrdersByUser(userId: String): Flow<List<Order>> = flow {
        emit(orders.filter { it.userId == userId })
    }

    override suspend fun getOrderByCode(orderCode: String): Order? =
        orders.find { it.orderCode == orderCode }

    override suspend fun insertOrder(order: Order) {
        orders.add(order)
    }

    override suspend fun updateOrder(order: Order) {
        val index = orders.indexOfFirst { it.id == order.id }
        if (index != -1) {
            orders[index] = order
        }
    }

    override suspend fun deleteOrder(order: Order) {
        orders.remove(order)
    }

    override suspend fun getOrderById(orderId: String): Order? =
        orders.find { it.id == orderId }

    override fun getOrdersByStatus(status: String): Flow<List<Order>> = flow {
        emit(orders.filter { it.status.name == status })
    }

    override suspend fun deleteAllOrders() {
        orders.clear()
    }
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
    
    override suspend fun clearAndRefreshDatabase() {}
} 