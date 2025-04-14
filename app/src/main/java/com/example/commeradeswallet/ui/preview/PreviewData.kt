package com.example.commeradeswallet.ui.preview

import com.example.commeradeswallet.data.model.FoodItem
import com.example.commeradeswallet.data.model.WalletTransaction
import com.example.commeradeswallet.data.model.TransactionType
import java.time.LocalDateTime

object PreviewData {
    val foodItems = listOf(
        FoodItem(
            id = "1",
            name = "Chapati",
            price = 20,
            category = "Breakfast",
            imageUrl = "",
            description = "Delicious chapati",
            isQuantifiedByNumber = true
        ),
        FoodItem(
            id = "2",
            name = "Rice",
            price = 50,
            category = "Lunch",
            imageUrl = "",
            description = "Steamed rice",
            isQuantifiedByNumber = false
        ),
        FoodItem(
            id = "3",
            name = "Beans",
            price = 40,
            category = "Lunch",
            imageUrl = "",
            description = "Cooked beans",
            isQuantifiedByNumber = false
        ),
        FoodItem(
            id = "4",
            name = "Beef Stew",
            price = 120,
            category = "Lunch",
            imageUrl = "",
            description = "Beef stew",
            isQuantifiedByNumber = false
        )
    )

    val sampleTransactions = listOf(
        WalletTransaction(
            id = 1,
            userId = "preview_user_id",
            amount = 500.0,
            type = TransactionType.DEPOSIT,
            description = "M-Pesa Top-up",
            timestamp = LocalDateTime.now(),
            reference = "QWE123456"
        ),
        WalletTransaction(
            id = 2,
            userId = "preview_user_id",
            amount = 150.0,
            type = TransactionType.WITHDRAWAL,
            description = "Food Order #ABC123",
            timestamp = LocalDateTime.now().minusHours(2)
        ),
        WalletTransaction(
            id = 3,
            userId = "preview_user_id",
            amount = 1000.0,
            type = TransactionType.DEPOSIT,
            description = "M-Pesa Top-up",
            timestamp = LocalDateTime.now().minusDays(1),
            reference = "XYZ789012"
        )
    )
} 