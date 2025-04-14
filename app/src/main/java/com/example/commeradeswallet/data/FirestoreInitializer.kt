package com.example.commeradeswallet.data

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.commeradeswallet.data.model.*
import com.example.commeradeswallet.data.repository.FirestoreRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDateTime
import java.security.MessageDigest

class FirestoreInitializer(private val context: Context) {
    private val repository = FirestoreRepository()
    private val scope = CoroutineScope(Dispatchers.IO)
    private val firestore = FirebaseFirestore.getInstance()
    private val foodCollection = firestore.collection("food_items")

    fun initializeData() {
        scope.launch {
            try {
                // Initialize food items
                initializeFirestore()
                Log.d("FirestoreInitializer", "Food items initialized successfully")
            } catch (e: Exception) {
                Log.e("FirestoreInitializer", "Error initializing food items", e)
            }
        }
    }

    private suspend fun initializeFirestore() {
        try {
            // Check if collection is empty
            val snapshot = foodCollection.get().await()
            if (snapshot.isEmpty) {
                Log.d("FirestoreInitializer", "Initializing Firestore with default data")
                insertDefaultFoodItems()
            } else {
                Log.d("FirestoreInitializer", "Firestore already initialized")
            }
        } catch (e: Exception) {
            Log.e("FirestoreInitializer", "Error initializing Firestore", e)
        }
    }

    private suspend fun insertDefaultFoodItems() {
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
            defaultItems.forEach { foodItem ->
                foodCollection.document(foodItem.id).set(foodItem).await()
            }
            Log.d("FirestoreInitializer", "Successfully inserted default food items")
        } catch (e: Exception) {
            Log.e("FirestoreInitializer", "Error inserting default food items", e)
        }
    }

    private fun hashPassword(password: String): String {
        val bytes = password.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }

    // Public function to create test data
    @RequiresApi(Build.VERSION_CODES.O)
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
