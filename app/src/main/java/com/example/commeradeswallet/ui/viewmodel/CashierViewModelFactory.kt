package com.example.commeradeswallet.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.commeradeswallet.data.AppDatabase
import com.example.commeradeswallet.data.repository.FirestoreRepository

class CashierViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CashierViewModel::class.java)) {
            val database = AppDatabase.getDatabase(context)
            return CashierViewModel(
                orderDao = database.orderDao(),
                foodDao = database.foodDao(),
                walletDao = database.walletDao(),
                firestoreRepository = FirestoreRepository()
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
} 