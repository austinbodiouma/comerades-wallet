package com.example.commeradeswallet.ui.preview

import com.example.commeradeswallet.data.model.FoodItem
import com.example.commeradeswallet.data.model.WalletTransaction
import com.example.commeradeswallet.data.model.TransactionType
import java.time.LocalDateTime

object PreviewData {
    val foodItems = listOf(
        FoodItem(
            id = 1,
            name = "Chapati",
            price = 15,
            category = "Main Course",
            imageUrl = "",
            description = "Fresh homemade chapati",
            isQuantifiedByNumber = true
        ),
        FoodItem(
            id = 2,
            name = "Rice",
            price = 30,
            category = "Main Course",
            imageUrl = "",
            description = "Steamed rice per serving",
            isQuantifiedByNumber = false
        ),
        FoodItem(
            id = 3,
            name = "Beef Stew",
            price = 75,
            category = "Stew",
            imageUrl = "",
            description = "Rich beef stew per serving",
            isQuantifiedByNumber = false
        ),
        FoodItem(
            id = 4,
            name = "Kales",
            price = 15,
            category = "Vegetables",
            imageUrl = "",
            description = "Fresh sukuma wiki per serving",
            isQuantifiedByNumber = false
        )
    )

    val sampleTransactions = listOf(
        WalletTransaction(
            id = 1,
            amount = 500.0,
            type = TransactionType.DEPOSIT,
            description = "M-Pesa Top-up",
            timestamp = LocalDateTime.now(),
            reference = "QWE123456"
        ),
        WalletTransaction(
            id = 2,
            amount = 150.0,
            type = TransactionType.WITHDRAWAL,
            description = "Food Order #ABC123",
            timestamp = LocalDateTime.now().minusHours(2)
        ),
        WalletTransaction(
            id = 3,
            amount = 1000.0,
            type = TransactionType.DEPOSIT,
            description = "M-Pesa Top-up",
            timestamp = LocalDateTime.now().minusDays(1),
            reference = "XYZ789012"
        )
    )
} 