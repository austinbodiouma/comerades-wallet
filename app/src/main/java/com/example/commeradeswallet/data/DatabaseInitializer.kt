package com.example.commeradeswallet.data

import android.content.Context
import androidx.startup.Initializer
import com.example.commeradeswallet.data.model.FoodItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class DatabaseInitializer : Initializer<AppDatabase> {
    override fun create(context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    override fun dependencies(): List<Class<out Initializer<*>>> {
        return emptyList()
    }

    companion object {
        private val initialFoodItems = listOf(
            FoodItem(
                name = "Chapati",
                price = 15,
                category = "Main Course",
                imageUrl = "",
                description = "Fresh homemade chapati",
                isQuantifiedByNumber = true
            ),
            FoodItem(
                name = "Rice",
                price = 30,
                category = "Main Course",
                imageUrl = "",
                description = "Steamed rice per serving",
                isQuantifiedByNumber = false
            ),
            FoodItem(
                name = "Beans",
                price = 20,
                category = "Main Course",
                imageUrl = "",
                description = "Well cooked beans",
                isQuantifiedByNumber = false
            ),
            FoodItem(
                name = "Githeri",
                price = 45,
                category = "Main Course",
                imageUrl = "",
                description = "Traditional githeri",
                isQuantifiedByNumber = false
            ),
            FoodItem(
                name = "Kales",
                price = 15,
                category = "Vegetables",
                imageUrl = "",
                description = "Fresh sukuma wiki",
                isQuantifiedByNumber = false
            ),
            FoodItem(
                name = "Cabbage",
                price = 10,
                category = "Vegetables",
                imageUrl = "",
                description = "Fresh cabbage per serving",
                isQuantifiedByNumber = false
            ),
            FoodItem(
                name = "Beef Stew",
                price = 75,
                category = "Stew",
                imageUrl = "",
                description = "Rich beef stew",
                isQuantifiedByNumber = false
            )
        )
    }
} 