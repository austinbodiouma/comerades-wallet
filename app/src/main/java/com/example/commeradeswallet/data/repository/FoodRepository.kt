package com.example.commeradeswallet.data.repository

import android.util.Log
import com.example.commeradeswallet.data.dao.FoodDao
import com.example.commeradeswallet.data.model.FoodItem
import com.example.commeradeswallet.ui.preview.PreviewData
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.tasks.await

interface FoodRepository {
    fun getAllFoodItems(): Flow<List<FoodItem>>
    fun getFoodItemsByCategory(category: String): Flow<List<FoodItem>>
    fun searchFoodItems(query: String): Flow<List<FoodItem>>
    suspend fun refreshFoodItems()
}

class RoomFoodRepository(
    private val foodDao: FoodDao,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : FoodRepository {
    
    override fun getAllFoodItems(): Flow<List<FoodItem>> {
        return foodDao.getAllFoodItems()
            .onEach { items ->
                if (items.isEmpty()) {
                    refreshFoodItems()
                }
            }
            .catch { e ->
                Log.e("FoodRepository", "Error getting food items", e)
                emit(emptyList())
            }
    }

    override fun getFoodItemsByCategory(category: String): Flow<List<FoodItem>> = 
        foodDao.getFoodItemsByCategory(category)

    override fun searchFoodItems(query: String): Flow<List<FoodItem>> = 
        foodDao.searchFoodItems(query)

    override suspend fun refreshFoodItems() {
        try {
            val foodItems = firestore.collection("food_items")
                .get()
                .await()
                .documents
                .mapNotNull { doc ->
                    try {
                        val item = doc.toObject(FoodItem::class.java)
                        item?.copy(id = doc.id.toIntOrNull() ?: 0)
                    } catch (e: Exception) {
                        Log.e("FoodRepository", "Error parsing food item", e)
                        null
                    }
                }
            
            if (foodItems.isNotEmpty()) {
                foodDao.insertAll(foodItems)
                Log.d("FoodRepository", "Successfully refreshed ${foodItems.size} food items")
            }
        } catch (e: Exception) {
            Log.e("FoodRepository", "Error refreshing food items", e)
            // If Firestore fetch fails, we'll still have the local cache available
        }
    }
}

class PreviewFoodRepository : FoodRepository {
    override fun getAllFoodItems() = flow { emit(PreviewData.foodItems) }
    override fun getFoodItemsByCategory(category: String) = 
        flow { emit(PreviewData.foodItems.filter { it.category == category }) }
    override fun searchFoodItems(query: String) = 
        flow { emit(PreviewData.foodItems.filter { 
            it.name.contains(query, ignoreCase = true) 
        }) }
    override suspend fun refreshFoodItems() {} // No-op for preview
} 