package com.example.commeradeswallet.ui.screens.wallet

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.commeradeswallet.data.AppDatabase
import com.example.commeradeswallet.data.repository.WalletRepository

class WalletViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WalletViewModel::class.java)) {
            val database = AppDatabase.getDatabase(context)
            val repository = WalletRepository(
                walletDao = database.walletDao(),
                orderDao = database.orderDao()
            )
            return WalletViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
} 