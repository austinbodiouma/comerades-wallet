package com.example.chef.data.repository

import android.util.Log
import com.example.chef.data.model.MenuItem
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class MenuRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val menuCollection = firestore.collection("food_items")

    fun getMenuItemsFlow(): Flow<List<MenuItem>> = callbackFlow {
        val subscription = menuCollection
            .orderBy("category")
            .orderBy("name")  // Secondary ordering by name
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("MenuRepository", "Error getting menu items", error)
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    // Use LinkedHashMap to maintain order while ensuring uniqueness by ID
                    val itemsMap = LinkedHashMap<String, MenuItem>()
                    
                    snapshot.documents.forEach { doc ->
                        try {
                            val id = doc.id
                            // Only process if we haven't seen this ID before
                            if (!itemsMap.containsKey(id)) {
                                val item = MenuItem(
                                    id = id,
                                    name = doc.getString("name") ?: "",
                                    price = doc.getLong("price")?.toInt() ?: 0,
                                    category = doc.getString("category") ?: "",
                                    imageUrl = doc.getString("imageUrl") ?: "",
                                    description = doc.getString("description") ?: "",
                                    isQuantifiedByNumber = doc.getBoolean("isQuantifiedByNumber") ?: false,
                                    isAvailable = doc.getBoolean("isAvailable") ?: true,
                                    lastUpdated = doc.getLong("lastUpdated") ?: System.currentTimeMillis()
                                )
                                itemsMap[id] = item
                            }
                        } catch (e: Exception) {
                            Log.e("MenuRepository", "Error parsing menu item: ${e.message}", e)
                        }
                    }
                    
                    val items = itemsMap.values.toList()
                    Log.d("MenuRepository", "Processed ${snapshot.documents.size} documents into ${items.size} unique menu items")
                    trySend(items)
                } else {
                    Log.d("MenuRepository", "No menu items available")
                    trySend(emptyList())
                }
            }

        awaitClose { subscription.remove() }
    }

    suspend fun updateItemAvailability(itemId: String, isAvailable: Boolean) {
        try {
            Log.d("MenuRepository", "Updating availability for item $itemId to $isAvailable")
            
            // Use a batch operation to ensure atomicity between related operations
            val batch = firestore.batch()
            
            // Update food_items collection
            val foodItemRef = firestore.collection("food_items").document(itemId)
            batch.update(foodItemRef, "isAvailable", isAvailable, "lastUpdated", System.currentTimeMillis())
            
            // Get the food item details first to ensure it exists
            val foodDoc = foodItemRef.get().await()
            
            if (foodDoc.exists()) {
                // Update or create the corresponding stock_items document
                val stockItemRef = firestore.collection("stock_items").document(itemId)
                
                // Get current stock document if it exists
                val stockDoc = stockItemRef.get().await()
                
                val currentQuantity = if (stockDoc.exists()) {
                    (stockDoc.getLong("quantity") ?: 0).toInt()
                } else {
                    0
                }
                
                val stockData = hashMapOf(
                    "id" to itemId,
                    "name" to (foodDoc.getString("name") ?: ""),
                    "quantity" to if (isAvailable) maxOf(currentQuantity, 1) else 0,
                    "unit" to "serving",
                    "minimumQuantity" to 0,
                    "category" to (foodDoc.getString("category") ?: ""),
                    "lastUpdated" to System.currentTimeMillis()
                )
                
                // Add to batch
                batch.set(stockItemRef, stockData)
                
                // Commit the batch operation
                batch.commit().await()
                
                Log.d("MenuRepository", "Successfully updated availability for $itemId across all collections")
            } else {
                // If the food item doesn't exist, we can't update stock - just update the food item
                Log.w("MenuRepository", "Food item $itemId not found, only updating availability")
                firestore.collection("food_items")
                    .document(itemId)
                    .update("isAvailable", isAvailable)
                    .await()
            }
        } catch (e: Exception) {
            Log.e("MenuRepository", "Failed to update item availability: ${e.message}", e)
            throw e
        }
    }

    suspend fun updateMenuItem(item: MenuItem) {
        try {
            Log.d("MenuRepository", "Updating food item ${item.id}")
            val itemData = mapOf(
                "name" to item.name,
                "price" to item.price,
                "category" to item.category,
                "imageUrl" to item.imageUrl,
                "description" to item.description,
                "isQuantifiedByNumber" to item.isQuantifiedByNumber,
                "isAvailable" to item.isAvailable,
                "lastUpdated" to item.lastUpdated
            )
            menuCollection.document(item.id)
                .set(itemData)
                .await()
            Log.d("MenuRepository", "Successfully updated food item ${item.id}")
        } catch (e: Exception) {
            Log.e("MenuRepository", "Error updating food item: ${e.message}", e)
            throw e
        }
    }

    suspend fun getMenuItems(): List<MenuItem> {
        return try {
            // Simple query without complex ordering to avoid index requirements
            val snapshot = menuCollection
                .get()
                .await()
            
            // Use a LinkedHashMap to maintain order while ensuring unique IDs
            val itemsMap = LinkedHashMap<String, MenuItem>()
            
            snapshot.documents.forEach { document ->
                try {
                    val id = document.id
                    // Only process if we haven't seen this ID before
                    if (!itemsMap.containsKey(id)) {
                        val item = document.toObject(MenuItem::class.java)
                        if (item != null) {
                            item.id = id
                            itemsMap[id] = item
                        }
                    }
                } catch (e: Exception) {
                    Log.e("MenuRepository", "Error converting document ${document.id}: ${e.message}")
                }
            }
            
            val items = itemsMap.values.toList()
            Log.d("MenuRepository", "Loaded ${items.size} unique menu items from ${snapshot.documents.size} documents")
            items
        } catch (e: Exception) {
            Log.e("MenuRepository", "Error getting menu items: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun getMenuItemsByCategory(category: String): List<MenuItem> {
        return try {
            // Simple query with single field filter to avoid index requirements
            val snapshot = menuCollection
                .whereEqualTo("category", category)
                .get()
                .await()
            
            // Use a LinkedHashMap to maintain order while ensuring unique IDs
            val itemsMap = LinkedHashMap<String, MenuItem>()
            
            snapshot.documents.forEach { document ->
                try {
                    val id = document.id
                    // Only process if we haven't seen this ID before
                    if (!itemsMap.containsKey(id)) {
                        val item = document.toObject(MenuItem::class.java)
                        if (item != null) {
                            item.id = id
                            itemsMap[id] = item
                        }
                    }
                } catch (e: Exception) {
                    Log.e("MenuRepository", "Error converting document ${document.id}: ${e.message}")
                }
            }
            
            val items = itemsMap.values.toList().sortedBy { it.name }
            Log.d("MenuRepository", "Loaded ${items.size} unique menu items for category: $category from ${snapshot.documents.size} documents")
            items
        } catch (e: Exception) {
            Log.e("MenuRepository", "Error getting menu items by category: ${e.message}", e)
            emptyList()
        }
    }

    // Get all categories from the menu items
    suspend fun getCategories(): List<String> {
        return try {
            val snapshot = menuCollection.get().await()
            val categories = snapshot.documents
                .mapNotNull { it.getString("category") }
                .distinct()
                .sorted()
                
            Log.d("MenuRepository", "Loaded ${categories.size} unique categories")
            categories
        } catch (e: Exception) {
            Log.e("MenuRepository", "Error getting categories: ${e.message}", e)
            emptyList()
        }
    }

    // Add more food categories to ensure we don't just have vegetables
    suspend fun addDefaultCategories() {
        val defaultCategories = listOf(
            "Main Dishes",
            "Vegetables",
            "Fruits",
            "Desserts",
            "Beverages"
        )
        
        // Check if we have enough categories already
        val existingCategories = getCategories()
        if (existingCategories.size >= 3) {
            Log.d("MenuRepository", "Enough categories exist already, skipping defaults")
            return
        }
        
        // Add some sample items for missing categories
        defaultCategories.forEach { category ->
            if (!existingCategories.contains(category)) {
                try {
                    val sampleItem = MenuItem(
                        name = "Sample $category Item",
                        category = category,
                        price = 100,
                        description = "Sample food item in the $category category",
                        isAvailable = true
                    )
                    
                    firestore.collection("food_items")
                        .add(sampleItem)
                        .await()
                    
                    Log.d("MenuRepository", "Added sample item for category: $category")
                } catch (e: Exception) {
                    Log.e("MenuRepository", "Error adding sample item for $category: ${e.message}")
                }
            }
        }
    }
} 