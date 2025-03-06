package com.example.commeradeswallet.data.dao

import androidx.room.*
import com.example.commeradeswallet.data.model.FoodItem
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodDao {
    @Query("SELECT * FROM food_items")
    fun getAllFoodItems(): Flow<List<FoodItem>>

    @Query("SELECT * FROM food_items WHERE category = :category")
    fun getFoodItemsByCategory(category: String): Flow<List<FoodItem>>

    @Query("SELECT * FROM food_items WHERE name LIKE '%' || :query || '%'")
    fun searchFoodItems(query: String): Flow<List<FoodItem>>

    @Query("SELECT COUNT(*) FROM food_items")
    suspend fun getFoodItemCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(foodItems: List<FoodItem>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(foodItem: FoodItem)

    @Update
    suspend fun update(foodItem: FoodItem)

    @Delete
    suspend fun delete(foodItem: FoodItem)
} 