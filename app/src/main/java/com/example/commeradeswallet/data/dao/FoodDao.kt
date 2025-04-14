package com.example.commeradeswallet.data.dao

import androidx.room.*
import com.example.commeradeswallet.data.model.FoodItem
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodDao {
    @Query("SELECT * FROM food_items ORDER BY lastUpdated DESC")
    fun getAllFoodItems(): Flow<List<FoodItem>>

    @Query("SELECT * FROM food_items ORDER BY lastUpdated DESC")
    suspend fun getAllFoodItemsSync(): List<FoodItem>

    @Query("SELECT * FROM food_items WHERE category = :category ORDER BY lastUpdated DESC")
    fun getFoodItemsByCategory(category: String): Flow<List<FoodItem>>

    @Query("SELECT * FROM food_items WHERE name LIKE '%' || :query || '%' ORDER BY lastUpdated DESC")
    fun searchFoodItems(query: String): Flow<List<FoodItem>>

    @Query("SELECT COUNT(*) FROM food_items")
    suspend fun getFoodItemCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(foodItems: List<FoodItem>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(foodItem: FoodItem)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(foodItem: FoodItem)

    @Delete
    suspend fun delete(foodItem: FoodItem)

    @Query("DELETE FROM food_items")
    suspend fun clearAllItems()

    @Transaction
    suspend fun replaceAllItems(foodItems: List<FoodItem>) {
        clearAllItems()
        insertAll(foodItems)
    }
} 