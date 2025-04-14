package com.example.commeradeswallet.data.dao

import androidx.room.*
import com.example.commeradeswallet.data.model.StockItem
import kotlinx.coroutines.flow.Flow

@Dao
interface StockDao {
    @Query("SELECT * FROM stock_items ORDER BY name ASC")
    fun getAllStockItems(): Flow<List<StockItem>>

    @Query("SELECT * FROM stock_items WHERE quantity <= minimumQuantity")
    fun getLowStockItems(): Flow<List<StockItem>>

    @Query("SELECT * FROM stock_items WHERE category = :category")
    fun getStockItemsByCategory(category: String): Flow<List<StockItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStockItem(item: StockItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<StockItem>)

    @Update
    suspend fun updateStockItem(item: StockItem)

    @Delete
    suspend fun deleteStockItem(item: StockItem)

    @Query("UPDATE stock_items SET quantity = :quantity, lastUpdated = :timestamp WHERE id = :itemId")
    suspend fun updateQuantity(itemId: Int, quantity: Int, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT * FROM stock_items WHERE id = :id")
    suspend fun getStockItemById(id: Int): StockItem?
} 