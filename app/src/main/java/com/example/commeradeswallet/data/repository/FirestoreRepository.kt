package com.example.commeradeswallet.data.repository

import android.util.Log
import com.example.commeradeswallet.data.model.*
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.tasks.await
import java.time.LocalDateTime
import java.time.ZoneId
import com.google.firebase.firestore.SetOptions

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
        
        // Create wallet for new user
        createWallet(userDoc.id)
        
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

    // Wallet Operations
    private suspend fun createWallet(userId: String) = try {
        val walletData = hashMapOf(
            "userId" to userId,
            "balance" to 0.0,
            "createdAt" to Timestamp.now(),
            "updatedAt" to Timestamp.now()
        )
        walletsCollection.document(userId).set(walletData).await()
    } catch (e: Exception) {
        Log.e("FirestoreRepository", "Error creating wallet", e)
        throw e
    }

    suspend fun getWallet(userId: String): Result<Double> = try {
        val doc = walletsCollection.document(userId).get().await()
        if (doc.exists()) {
            Result.success(doc.getDouble("balance") ?: 0.0)
        } else {
            Result.failure(Exception("Wallet not found"))
        }
    } catch (e: Exception) {
        Log.e("FirestoreRepository", "Error getting wallet", e)
        Result.failure(e)
    }

    // Transaction Operations
    suspend fun addTransaction(transaction: WalletTransaction): Result<String> = try {
        val transactionDoc = transactionsCollection.document()
        val transactionData = hashMapOf(
            "userId" to transaction.userId,
            "amount" to transaction.amount,
            "type" to transaction.type.name,
            "description" to transaction.description,
            "timestamp" to Timestamp.now(),
            "reference" to transaction.reference,
            "status" to "COMPLETED"
        )
        transactionDoc.set(transactionData).await()
        
        // Update wallet balance
        firestore.runTransaction { tx ->
            val walletRef = walletsCollection.document(transaction.userId)
            val wallet = tx.get(walletRef)
            val currentBalance = wallet.getDouble("balance") ?: 0.0
            val newBalance = when (transaction.type) {
                TransactionType.DEPOSIT -> currentBalance + transaction.amount
                TransactionType.WITHDRAWAL -> currentBalance - transaction.amount
            }
            tx.update(walletRef, "balance", newBalance)
        }.await()
        
        Result.success(transactionDoc.id)
    } catch (e: Exception) {
        Log.e("FirestoreRepository", "Error adding transaction", e)
        Result.failure(e)
    }

    // Food Items Operations
    suspend fun addFoodItem(foodItem: FoodItem): Result<String> = try {
        val foodDoc = foodItemsCollection.document()
        val foodData = hashMapOf(
            "name" to foodItem.name,
            "price" to foodItem.price,
            "category" to foodItem.category,
            "imageUrl" to foodItem.imageUrl,
            "description" to foodItem.description,
            "isQuantifiedByNumber" to foodItem.isQuantifiedByNumber
        )
        foodDoc.set(foodData).await()
        Result.success(foodDoc.id)
    } catch (e: Exception) {
        Log.e("FirestoreRepository", "Error adding food item", e)
        Result.failure(e)
    }

    suspend fun getAllFoodItems(): Result<List<FoodItem>> = try {
        val snapshot = foodItemsCollection.get().await()
        val foodItems = snapshot.documents.mapNotNull { doc ->
            try {
                FoodItem(
                    id = doc.id.toIntOrNull() ?: 0,
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
    suspend fun createOrder(order: Order): Result<String> = try {
        val orderDoc = ordersCollection.document()
        val orderData = hashMapOf(
            "userId" to order.userId,
            "items" to order.items.map { item ->
                hashMapOf(
                    "foodItemId" to item.foodItem.id,
                    "quantity" to item.quantity
                )
            },
            "totalAmount" to order.totalAmount,
            "status" to order.status.name,
            "timestamp" to Timestamp.now()
        )
        orderDoc.set(orderData).await()
        Result.success(orderDoc.id)
    } catch (e: Exception) {
        Log.e("FirestoreRepository", "Error creating order", e)
        Result.failure(e)
    }

    suspend fun getOrdersByUser(userId: String): Result<List<Order>> = try {
        val snapshot = ordersCollection
            .whereEqualTo("userId", userId)
            .orderBy("timestamp")
            .get()
            .await()
            
        val orders = snapshot.documents.mapNotNull { doc ->
            try {
                Order(
                    id = doc.id.toLongOrNull() ?: return@mapNotNull null,
                    userId = doc.getString("userId") ?: return@mapNotNull null,
                    items = (doc.get("items") as? List<Map<String, Any>>)?.map { item ->
                        CartItem(
                            foodItem = FoodItem(
                                id = (item["foodItemId"] as? Long)?.toInt() ?: return@mapNotNull null,
                                name = "", // These will need to be populated from the food items collection
                                price = 0,
                                category = "",
                                imageUrl = "",
                                description = ""
                            ),
                            quantity = (item["quantity"] as? Long)?.toInt() ?: return@mapNotNull null
                        )
                    } ?: return@mapNotNull null,
                    totalAmount = doc.getDouble("totalAmount") ?: return@mapNotNull null,
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

    suspend fun updateOrderStatus(orderId: String, status: OrderStatus): Result<Unit> = try {
        ordersCollection.document(orderId)
            .update("status", status.name)
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e("FirestoreRepository", "Error updating order status", e)
        Result.failure(e)
    }
} 