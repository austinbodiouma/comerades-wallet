package com.example.commeradeswallet.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stock_items")
data class StockItem(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val quantity: Int,
    val unit: String,
    val minimumQuantity: Int = 0,
    val category: String,
    val lastUpdated: Long = System.currentTimeMillis()
) 