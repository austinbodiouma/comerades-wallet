package com.example.commeradeswallet.data

import android.content.Context
import android.util.Log
import androidx.startup.Initializer
import com.example.commeradeswallet.data.model.FoodItem
import com.example.commeradeswallet.data.repository.RoomFoodRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.commeradeswallet.ui.navigation.NavGraph

class DatabaseInitializer : Initializer<AppDatabase> {
    private var context: Context? = null
    
    // Required no-arg constructor
    constructor()
    
    // Secondary constructor for cases where context is passed explicitly
    constructor(context: Context) {
        this.context = context
    }
    
    override fun create(context: Context): AppDatabase {
        val database = AppDatabase.getDatabase(context)
        
        // Initialize database with food items
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val foodDao = database.foodDao()
                // Check if data already exists
                if (foodDao.getFoodItemCount() == 0) {
                    Log.d("DatabaseInitializer", "Inserting initial food items")
                    insertDefaultFoodItems(database)
                }
            } catch (e: Exception) {
                Log.e("DatabaseInitializer", "Error initializing database", e)
            }
        }
        
        return database
    }

    override fun dependencies(): List<Class<out Initializer<*>>> {
        return emptyList()
    }

    private suspend fun insertDefaultFoodItems(database: AppDatabase) {
        val defaultItems = listOf(
            FoodItem(
                id = "chapati",
                name = "Chapati",
                price = 20,
                category = "Breakfast",
                imageUrl = "",
                description = "Fresh homemade chapati",
                isQuantifiedByNumber = true
            ),
            FoodItem(
                id = "rice",
                name = "Rice",
                price = 50,
                category = "Lunch",
                imageUrl = "",
                description = "Steamed rice per serving",
                isQuantifiedByNumber = false
            ),
            FoodItem(
                id = "beans",
                name = "Beans",
                price = 40,
                category = "Lunch",
                imageUrl = "",
                description = "Cooked beans per serving",
                isQuantifiedByNumber = false
            ),
            FoodItem(
                id = "beef_stew",
                name = "Beef Stew",
                price = 120,
                category = "Lunch",
                imageUrl = "",
                description = "Rich beef stew per serving",
                isQuantifiedByNumber = false
            ),
            FoodItem(
                id = "kales",
                name = "Kales",
                price = 30,
                category = "Vegetables",
                imageUrl = "",
                description = "Fresh sukuma wiki per serving",
                isQuantifiedByNumber = false
            ),
            FoodItem(
                id = "cabbage",
                name = "Cabbage",
                price = 30,
                category = "Vegetables",
                imageUrl = "",
                description = "Fresh cabbage per serving",
                isQuantifiedByNumber = false
            ),
            FoodItem(
                id = "githeri",
                name = "Githeri",
                price = 60,
                category = "Lunch",
                imageUrl = "",
                description = "Mixed beans and maize",
                isQuantifiedByNumber = false
            )
        )

        try {
            database.foodDao().insertAll(defaultItems)
            Log.d("DatabaseInitializer", "Successfully inserted default food items")
        } catch (e: Exception) {
            Log.e("DatabaseInitializer", "Error inserting default food items", e)
        }
    }
} 