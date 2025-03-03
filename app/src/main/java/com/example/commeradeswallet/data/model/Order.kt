package com.example.commeradeswallet.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "orders")
data class Order(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val orderCode: String, // Unique order code
    val userId: String,
    val items: List<CartItem>,
    val totalAmount: Double,
    val status: OrderStatus,
    val timestamp: LocalDateTime = LocalDateTime.now()
)

enum class OrderStatus {
    PENDING, // Just placed
    CONFIRMED, // Accepted by cashier
    PREPARING, // In kitchen
    READY, // Ready for pickup
    COMPLETED, // Picked up
    CANCELLED
} 