package com.example.commeradeswallet.data.repository

import android.util.Log
import com.example.commeradeswallet.data.dao.FoodDao
import com.example.commeradeswallet.data.model.FoodItem
import com.example.commeradeswallet.ui.preview.PreviewData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach

interface FoodRepository {
    fun getAllFoodItems(): Flow<List<FoodItem>>
    fun getFoodItemsByCategory(category: String): Flow<List<FoodItem>>
    fun searchFoodItems(query: String): Flow<List<FoodItem>>
}

class RoomFoodRepository(private val foodDao: FoodDao) : FoodRepository {
    suspend fun checkDataExists(): Boolean {
        return foodDao.getFoodItemCount() > 0
    }

    override fun getAllFoodItems(): Flow<List<FoodItem>> {
        return foodDao.getAllFoodItems().onEach { items ->
            Log.d("RoomFoodRepository", "Retrieved ${items.size} food items from database")
        }
    }

    override fun getFoodItemsByCategory(category: String) = foodDao.getFoodItemsByCategory(category)
    override fun searchFoodItems(query: String) = foodDao.searchFoodItems(query)
}

class PreviewFoodRepository : FoodRepository {
    override fun getAllFoodItems() = flowOf(PreviewData.foodItems)
    override fun getFoodItemsByCategory(category: String) = 
        flowOf(PreviewData.foodItems.filter { it.category == category })
    override fun searchFoodItems(query: String) = 
        flowOf(PreviewData.foodItems.filter { 
            it.name.contains(query, ignoreCase = true) 
        })
} 