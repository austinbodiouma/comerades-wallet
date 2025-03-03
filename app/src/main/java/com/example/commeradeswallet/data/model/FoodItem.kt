package com.example.commeradeswallet.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "food_items")
data class FoodItem(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val price: Int,
    val category: String,
    val imageUrl: String,
    val description: String,
    val isQuantifiedByNumber: Boolean = false
) 