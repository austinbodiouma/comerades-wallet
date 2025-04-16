package com.example.commeradeswallet.data.repository

import android.util.Log
import com.example.commeradeswallet.data.dao.FoodDao
import com.example.commeradeswallet.data.model.FoodItem
import com.example.commeradeswallet.ui.preview.PreviewData
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
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
    suspend fun clearAndRefreshDatabase()
}

class RoomFoodRepository(
    private val foodDao: FoodDao,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : FoodRepository {
    
    init {
        // Set up real-time listener for availability changes
        setupAvailabilityListener()
    }
    
    private fun setupAvailabilityListener() {
        firestore.collection("food_items")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FoodRepository", "Error listening for food item changes", error)
                    return@addSnapshotListener
                }
                
                if (snapshot != null && !snapshot.isEmpty) {
                    // Process changes in a separate coroutine to avoid blocking the listener
                    GlobalScope.launch {
                        try {
                            val changedItems = mutableListOf<FoodItem>()
                            
                            for (change in snapshot.documentChanges) {
                                val doc = change.document
                                try {
                                    // Create food item from document
                                    val item = FoodItem(
                                        id = doc.id,
                                        name = doc.getString("name") ?: continue,
                                        price = doc.getLong("price")?.toInt() ?: continue,
                                        category = doc.getString("category") ?: continue,
                                        imageUrl = doc.getString("imageUrl") ?: "",
                                        description = doc.getString("description") ?: "",
                                        isQuantifiedByNumber = doc.getBoolean("isQuantifiedByNumber") ?: false,
                                        isAvailable = doc.getBoolean("isAvailable") ?: true,
                                        lastUpdated = doc.getLong("lastUpdated") ?: System.currentTimeMillis()
                                    )
                                    
                                    changedItems.add(item)
                                } catch (e: Exception) {
                                    Log.e("FoodRepository", "Error parsing changed food item ${doc.id}", e)
                                }
                            }
                            
                            if (changedItems.isNotEmpty()) {
                                // Update these specific items in the database
                                foodDao.insertAll(changedItems)
                                Log.d("FoodRepository", "Real-time update: ${changedItems.size} items updated")
                            }
                        } catch (e: Exception) {
                            Log.e("FoodRepository", "Error processing real-time updates", e)
                        }
                    }
                }
            }
    }
    
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
                    val isAvailable = doc.getBoolean("isAvailable") ?: true
                    
                    // Create food item object
                    FoodItem(
                        id = doc.id, // Use Firestore document ID directly
                        name = doc.getString("name") ?: return@mapNotNull null,
                        price = doc.getLong("price")?.toInt() ?: return@mapNotNull null,
                        category = doc.getString("category") ?: return@mapNotNull null,
                        imageUrl = doc.getString("imageUrl") ?: "",
                        description = doc.getString("description") ?: "",
                        isQuantifiedByNumber = doc.getBoolean("isQuantifiedByNumber") ?: false,
                        isAvailable = isAvailable,
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

    override suspend fun clearAndRefreshDatabase() {
        try {
            Log.d("FoodRepository", "Clearing database and refreshing from Firestore")
            
            // First clear the database
            foodDao.clearAllItems()
            
            // Then fetch fresh data from Firestore
            val snapshot = firestore.collection("food_items")
                .get()
                .await()
            
            // Process and deduplicate items
            val foodItemsMap = mutableMapOf<String, FoodItem>()
            
            snapshot.documents.forEach { doc ->
                try {
                    val id = doc.id
                    if (!foodItemsMap.containsKey(id)) {
                        foodItemsMap[id] = FoodItem(
                            id = id,
                            name = doc.getString("name") ?: return@forEach,
                            price = doc.getLong("price")?.toInt() ?: return@forEach,
                            category = doc.getString("category") ?: return@forEach,
                            imageUrl = doc.getString("imageUrl") ?: "",
                            description = doc.getString("description") ?: "",
                            isQuantifiedByNumber = doc.getBoolean("isQuantifiedByNumber") ?: false,
                            isAvailable = doc.getBoolean("isAvailable") ?: true,
                            lastUpdated = doc.getLong("lastUpdated") ?: System.currentTimeMillis()
                        )
                    }
                } catch (e: Exception) {
                    Log.e("FoodRepository", "Error parsing food item ${doc.id}: ${e.message}", e)
                }
            }
            
            val foodItems = foodItemsMap.values.toList()
            if (foodItems.isNotEmpty()) {
                // Insert the deduplicated items
                foodDao.insertAll(foodItems)
                Log.d("FoodRepository", "Successfully refreshed ${foodItems.size} unique food items")
            } else {
                Log.w("FoodRepository", "No valid food items found in Firestore")
            }
        } catch (e: Exception) {
            Log.e("FoodRepository", "Error clearing and refreshing database: ${e.message}", e)
            throw e
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
    override suspend fun clearAndRefreshDatabase() {} // No-op for preview
} 