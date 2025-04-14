package com.example.commeradeswallet.data.repository

import android.util.Log
import com.example.commeradeswallet.data.model.*
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FirestoreRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    // Collection References
    private val usersCollection = firestore.collection("users")
    private val walletsCollection = firestore.collection("wallets")
    private val transactionsCollection = firestore.collection("transactions")
    private val foodItemsCollection = firestore.collection("food_items")
    private val ordersCollection = firestore.collection("orders")

    // User Operations
    suspend fun createUser(user: User): Result<String> = try {
        val userDoc = usersCollection.document()
        val userData = hashMapOf(
            "email" to user.email,
            "password" to user.password,
            "name" to user.name,
            "phoneNumber" to user.phoneNumber,
            "authProvider" to user.authProvider,
            "createdAt" to Timestamp.now()
        )
        userDoc.set(userData).await()
        Result.success(userDoc.id)
    } catch (e: Exception) {
        Log.e("FirestoreRepository", "Error creating user", e)
        Result.failure(e)
    }

    suspend fun getUser(userId: String): Result<User> = try {
        val doc = usersCollection.document(userId).get().await()
        if (doc.exists()) {
            val user = User(
                id = doc.id,
                email = doc.getString("email") ?: "",
                password = doc.getString("password") ?: "",
                name = doc.getString("name"),
                phoneNumber = doc.getString("phoneNumber"),
                authProvider = doc.getString("authProvider") ?: "EMAIL"
            )
            Result.success(user)
        } else {
            Result.failure(Exception("User not found"))
        }
    } catch (e: Exception) {
        Log.e("FirestoreRepository", "Error getting user", e)
        Result.failure(e)
    }

    // Food Items Operations
    suspend fun addFoodItem(item: FoodItem): Result<String> {
        return try {
            val documentRef = if (item.id.isNotEmpty()) {
                foodItemsCollection.document(item.id)
            } else {
                foodItemsCollection.document()
            }
            
            documentRef.set(item).await()
            Result.success(documentRef.id)
        } catch (e: Exception) {
            Log.e("FirestoreRepository", "Error adding food item", e)
            Result.failure(e)
        }
    }

    suspend fun getAllFoodItems(): Result<List<FoodItem>> = try {
        val snapshot = foodItemsCollection.get().await()
        val foodItems = snapshot.documents.mapNotNull { doc ->
            try {
                FoodItem(
                    id = doc.id,
                    name = doc.getString("name") ?: return@mapNotNull null,
                    price = doc.getLong("price")?.toInt() ?: return@mapNotNull null,
                    category = doc.getString("category") ?: return@mapNotNull null,
                    imageUrl = doc.getString("imageUrl") ?: "",
                    description = doc.getString("description") ?: "",
                    isQuantifiedByNumber = doc.getBoolean("isQuantifiedByNumber") ?: false
                )
            } catch (e: Exception) {
                Log.e("FirestoreRepository", "Error parsing food item", e)
                null
            }
        }
        Result.success(foodItems)
    } catch (e: Exception) {
        Log.e("FirestoreRepository", "Error getting food items", e)
        Result.failure(e)
    }

    // Order Operations
    suspend fun getOrders(): Result<List<Order>> = try {
        val snapshot = ordersCollection.get().await()
        val orders = snapshot.documents.mapNotNull { doc ->
            try {
                Order(
                    id = doc.id,
                    userId = doc.getString("userId") ?: return@mapNotNull null,
                    items = (doc.get("items") as? List<Map<String, Any>>)?.mapNotNull { item ->
                        CartItem(
                            foodItem = FoodItem(
                                id = (item["foodItemId"] as? String) ?: return@mapNotNull null,
                                name = "", // These will need to be populated from the food items collection
                                price = 0,
                                category = "",
                                imageUrl = "",
                                description = ""
                            ),
                            quantity = (item["quantity"] as? Long)?.toInt() ?: return@mapNotNull null
                        )
                    } ?: return@mapNotNull null,
                    totalAmount = (doc.get("totalAmount") as? Number)?.toDouble() ?: return@mapNotNull null,
                    status = OrderStatus.valueOf(doc.getString("status") ?: return@mapNotNull null)
                )
            } catch (e: Exception) {
                Log.e("FirestoreRepository", "Error parsing order", e)
                null
            }
        }
        Result.success(orders)
    } catch (e: Exception) {
        Log.e("FirestoreRepository", "Error getting orders", e)
        Result.failure(e)
    }

    suspend fun updateFoodItem(item: FoodItem): Result<String> {
        return try {
            foodItemsCollection.document(item.id).set(item).await()
            Result.success(item.id)
        } catch (e: Exception) {
            Log.e("FirestoreRepository", "Error updating food item", e)
            Result.failure(e)
        }
    }

    suspend fun addTransaction(transaction: WalletTransaction): Result<String> = try {
        val transactionDoc = transactionsCollection.document()
        val transactionData: MutableMap<String, Any> = mutableMapOf(
            "userId" to transaction.userId,
            "amount" to transaction.amount,
            "type" to transaction.type.name,
            "description" to transaction.description,
            "timestamp" to Timestamp.now(),
            "reference" to (transaction.reference ?: ""),
            "status" to "COMPLETED" as String
        )
        transactionDoc.set(transactionData).await()
        
        Result.success(transactionDoc.id)
    } catch (e: Exception) {
        Log.e("FirestoreRepository", "Error adding transaction", e)
        Result.failure(e)
    }

    suspend fun processPayment(order: Order): Result<Unit> = try {
        // Implementation for processing payment
        // This is a placeholder for the actual payment processing logic
        Log.d("FirestoreRepository", "Processing payment for order: ${order.id}")
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e("FirestoreRepository", "Error processing payment", e)
        Result.failure(e)
    }

    suspend fun updateOrder(order: Order): Result<Unit> = try {
        val orderRef = ordersCollection.document(order.id)
        val orderData: MutableMap<String, Any> = mutableMapOf(
            "status" to order.status.name,
            "timestamp" to Timestamp.now()
        )
        orderRef.update(orderData).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e("FirestoreRepository", "Error updating order", e)
        Result.failure(e)
    }

    suspend fun updateStockQuantity(itemId: Int, quantity: Int, timestamp: Long): Result<Unit> = try {
        val updateData = hashMapOf(
            "quantity" to quantity,
            "lastUpdated" to timestamp
        )
        firestore.collection("stock_items")
            .document(itemId.toString())
            .update(updateData as Map<String, Any>)
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e("FirestoreRepository", "Error updating stock quantity", e)
        Result.failure(e)
    }

    suspend fun getAllTransactions(): Result<List<WalletTransaction>> = try {
        val snapshot = transactionsCollection.get().await()
        val transactions = snapshot.documents.mapNotNull { doc ->
            try {
                WalletTransaction(
                    id = doc.id.hashCode().toLong(),
                    userId = doc.getString("userId") ?: return@mapNotNull null,
                    amount = (doc.get("amount") as? Number)?.toDouble() ?: return@mapNotNull null,
                    type = TransactionType.valueOf(doc.getString("type") ?: return@mapNotNull null),
                    description = doc.getString("description") ?: "",
                    timestamp = (doc.get("timestamp") as? Timestamp)?.toDate()?.toInstant()
                        ?.atZone(ZoneId.systemDefault())
                        ?.toLocalDateTime() ?: LocalDateTime.now(),
                    reference = doc.getString("reference")
                )
            } catch (e: Exception) {
                Log.e("FirestoreRepository", "Error parsing transaction", e)
                null
            }
        }
        Result.success(transactions)
    } catch (e: Exception) {
        Log.e("FirestoreRepository", "Error getting transactions", e)
        Result.failure(e)
    }

    suspend fun getAllStockItems(): Result<List<StockItem>> = try {
        val snapshot = firestore.collection("stock_items")
            .orderBy("name")
            .get()
            .await()
        val items = snapshot.documents.mapNotNull { doc ->
            try {
                StockItem(
                    id = doc.id.toIntOrNull() ?: return@mapNotNull null,
                    name = doc.getString("name") ?: return@mapNotNull null,
                    quantity = (doc.get("quantity") as? Number)?.toInt() ?: return@mapNotNull null,
                    unit = doc.getString("unit") ?: return@mapNotNull null,
                    minimumQuantity = (doc.get("minimumQuantity") as? Number)?.toInt() ?: 0,
                    category = doc.getString("category") ?: return@mapNotNull null,
                    lastUpdated = (doc.get("lastUpdated") as? Number)?.toLong() ?: System.currentTimeMillis()
                )
            } catch (e: Exception) {
                Log.e("FirestoreRepository", "Error parsing stock item", e)
                null
            }
        }
        Result.success(items)
    } catch (e: Exception) {
        Log.e("FirestoreRepository", "Error getting stock items", e)
        Result.failure(e)
    }

    suspend fun getOrderByCode(orderCode: String): Result<Order?> = withContext(Dispatchers.IO) {
        try {
            val snapshot = ordersCollection
                .whereEqualTo("orderCode", orderCode)
                .get()
                .await()

            if (snapshot.isEmpty) {
                return@withContext Result.success(null)
            }

            val document = snapshot.documents.first()
            val order = document.toObject(Order::class.java)?.copy(id = document.id)
            Result.success(order)
        } catch (e: Exception) {
            Log.e("FirestoreRepository", "Error getting order by code", e)
            Result.failure(e)
        }
    }
}
