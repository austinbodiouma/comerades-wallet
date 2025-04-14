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
            Log.d("FoodRepository", "Refreshing food items from Firestore")
            
            // Fetch all items from Firestore
            val snapshot = firestore.collection("food_items")
                .get()
                .await()
            
            val foodItems = snapshot.documents.mapNotNull { doc ->
                try {
                    FoodItem(
                        id = doc.id, // Use Firestore document ID directly
                        name = doc.getString("name") ?: return@mapNotNull null,
                        price = doc.getLong("price")?.toInt() ?: return@mapNotNull null,
                        category = doc.getString("category") ?: return@mapNotNull null,
                        imageUrl = doc.getString("imageUrl") ?: "",
                        description = doc.getString("description") ?: "",
                        isQuantifiedByNumber = doc.getBoolean("isQuantifiedByNumber") ?: false,
                        isAvailable = doc.getBoolean("isAvailable") ?: true,
                        lastUpdated = doc.getLong("lastUpdated") ?: System.currentTimeMillis()
                    )
                } catch (e: Exception) {
                    Log.e("FoodRepository", "Error parsing food item ${doc.id}: ${e.message}", e)
                    null
                }
            }

            if (foodItems.isNotEmpty()) {
                // Replace all items in a single transaction
                foodDao.replaceAllItems(foodItems)
                Log.d("FoodRepository", "Successfully refreshed ${foodItems.size} food items")
            } else {
                Log.w("FoodRepository", "No valid food items found in Firestore")
            }
        } catch (e: Exception) {
            Log.e("FoodRepository", "Error refreshing food items: ${e.message}", e)
            throw e // Propagate the error to handle it in the ViewModel
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