package com.example.commeradeswallet.data

import android.content.Context
import android.util.Log
import com.example.commeradeswallet.data.model.*
import com.example.commeradeswallet.data.repository.FirestoreRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.security.MessageDigest

class FirestoreInitializer(private val context: Context) {
    private val repository = FirestoreRepository()
    private val scope = CoroutineScope(Dispatchers.IO)

    fun initializeData() {
        scope.launch {
            try {
                // Initialize food items
                initializeFoodItems()
                Log.d("FirestoreInitializer", "Food items initialized successfully")
            } catch (e: Exception) {
                Log.e("FirestoreInitializer", "Error initializing data", e)
            }
        }
    }

    private suspend fun initializeFoodItems() {
        val foodItems = listOf(
            FoodItem(
                name = "Chapati",
                price = 15,
                category = "Main Course",
                imageUrl = "https://firebasestorage.googleapis.com/v0/b/commerades-wallet.appspot.com/o/food%2Fchapati.jpg",
                description = "Fresh homemade chapati",
                isQuantifiedByNumber = true
            ),
            FoodItem(
                name = "Rice",
                price = 30,
                category = "Main Course",
                imageUrl = "https://firebasestorage.googleapis.com/v0/b/commerades-wallet.appspot.com/o/food%2Frice.jpg",
                description = "Steamed rice per serving",
                isQuantifiedByNumber = false
            ),
            FoodItem(
                name = "Beans",
                price = 20,
                category = "Main Course",
                imageUrl = "https://firebasestorage.googleapis.com/v0/b/commerades-wallet.appspot.com/o/food%2Fbeans.jpg",
                description = "Well cooked beans",
                isQuantifiedByNumber = false
            ),
            FoodItem(
                name = "Githeri",
                price = 45,
                category = "Main Course",
                imageUrl = "https://firebasestorage.googleapis.com/v0/b/commerades-wallet.appspot.com/o/food%2Fgitheri.jpg",
                description = "Traditional githeri",
                isQuantifiedByNumber = false
            ),
            FoodItem(
                name = "Beef Stew",
                price = 75,
                category = "Stew",
                imageUrl = "https://firebasestorage.googleapis.com/v0/b/commerades-wallet.appspot.com/o/food%2Fbeef_stew.jpg",
                description = "Rich beef stew per serving",
                isQuantifiedByNumber = false
            ),
            FoodItem(
                name = "Chicken Stew",
                price = 85,
                category = "Stew",
                imageUrl = "https://firebasestorage.googleapis.com/v0/b/commerades-wallet.appspot.com/o/food%2Fchicken_stew.jpg",
                description = "Delicious chicken stew",
                isQuantifiedByNumber = false
            ),
            FoodItem(
                name = "Kales (Sukuma Wiki)",
                price = 15,
                category = "Vegetables",
                imageUrl = "https://firebasestorage.googleapis.com/v0/b/commerades-wallet.appspot.com/o/food%2Fkales.jpg",
                description = "Fresh sukuma wiki per serving",
                isQuantifiedByNumber = false
            ),
            FoodItem(
                name = "Cabbage",
                price = 10,
                category = "Vegetables",
                imageUrl = "https://firebasestorage.googleapis.com/v0/b/commerades-wallet.appspot.com/o/food%2Fcabbage.jpg",
                description = "Fresh cabbage per serving",
                isQuantifiedByNumber = false
            ),
            FoodItem(
                name = "Ugali",
                price = 20,
                category = "Main Course",
                imageUrl = "https://firebasestorage.googleapis.com/v0/b/commerades-wallet.appspot.com/o/food%2Fugali.jpg",
                description = "Traditional ugali per serving",
                isQuantifiedByNumber = false
            ),
            FoodItem(
                name = "Matumbo",
                price = 65,
                category = "Stew",
                imageUrl = "https://firebasestorage.googleapis.com/v0/b/commerades-wallet.appspot.com/o/food%2Fmatumbo.jpg",
                description = "Well-cooked matumbo stew",
                isQuantifiedByNumber = false
            )
        )

        foodItems.forEach { foodItem ->
            try {
                repository.addFoodItem(foodItem)
                Log.d("FirestoreInitializer", "Added food item: ${foodItem.name}")
            } catch (e: Exception) {
                Log.e("FirestoreInitializer", "Error adding food item: ${foodItem.name}", e)
            }
        }
    }

    private fun hashPassword(password: String): String {
        val bytes = password.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }

    // This function can be called to create a test user with some sample transactions
    suspend fun createTestData(email: String, name: String) {
        try {
            // Create test user with hashed password
            val user = User(
                email = email,
                password = hashPassword("test123"), // Default test password
                name = name,
                phoneNumber = null,
                authProvider = "EMAIL"
            )
            
            val userResult = repository.createUser(user)
            if (userResult.isSuccess) {
                val userId = userResult.getOrNull()!!
                
                // Add some test transactions
                val transactions = listOf(
                    WalletTransaction(
                        userId = userId,
                        amount = 500.0,
                        type = TransactionType.DEPOSIT,
                        description = "Initial deposit",
                        timestamp = LocalDateTime.now(),
                        reference = "TEST001"
                    ),
                    WalletTransaction(
                        userId = userId,
                        amount = 150.0,
                        type = TransactionType.WITHDRAWAL,
                        description = "Food purchase",
                        timestamp = LocalDateTime.now().minusHours(2),
                        reference = "TEST002"
                    ),
                    WalletTransaction(
                        userId = userId,
                        amount = 1000.0,
                        type = TransactionType.DEPOSIT,
                        description = "M-Pesa Top-up",
                        timestamp = LocalDateTime.now().minusDays(1),
                        reference = "TEST003"
                    )
                )

                transactions.forEach { transaction ->
                    repository.addTransaction(transaction)
                }

                Log.d("FirestoreInitializer", "Test data created successfully for user: $userId")
            }
        } catch (e: Exception) {
            Log.e("FirestoreInitializer", "Error creating test data", e)
        }
    }
} 