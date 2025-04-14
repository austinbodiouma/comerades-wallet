package com.example.commeradeswallet.data.dao

import androidx.room.*
import com.example.commeradeswallet.data.model.Order
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderDao {
    @Query("SELECT * FROM orders ORDER BY timestamp DESC")
    fun getAllOrders(): Flow<List<Order>>

    @Query("SELECT * FROM orders WHERE userId = :userId ORDER BY timestamp DESC")
    fun getOrdersByUser(userId: String): Flow<List<Order>>

    @Query("SELECT * FROM orders WHERE orderCode = :orderCode")
    suspend fun getOrderByCode(orderCode: String): Order?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: Order)

    @Update
    suspend fun updateOrder(order: Order)

    @Delete
    suspend fun deleteOrder(order: Order)

    @Query("SELECT * FROM orders WHERE id = :orderId")
    suspend fun getOrderById(orderId: String): Order?

    @Query("SELECT * FROM orders WHERE status = :status ORDER BY timestamp DESC")
    fun getOrdersByStatus(status: String): Flow<List<Order>>

    @Query("DELETE FROM orders")
    suspend fun deleteAllOrders()
} 