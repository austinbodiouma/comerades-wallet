package com.example.commeradeswallet.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.commeradeswallet.data.converter.Converters
import java.time.LocalDateTime

@Entity(tableName = "orders")
@TypeConverters(Converters::class)
data class Order(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String,
    val orderCode: String? = null,
    val items: List<CartItem>,
    val totalAmount: Double,
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val status: OrderStatus = OrderStatus.PENDING
)

enum class OrderStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    CANCELLED
} 