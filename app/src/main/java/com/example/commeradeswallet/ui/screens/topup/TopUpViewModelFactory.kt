package com.example.commeradeswallet.ui.screens.topup

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.commeradeswallet.data.AppDatabase
import com.example.commeradeswallet.data.repository.MpesaRepository
import com.example.commeradeswallet.data.repository.WalletRepository
import com.google.firebase.firestore.FirebaseFirestore

class TopUpViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TopUpViewModel::class.java)) {
            val database = AppDatabase.getDatabase(context)
            val walletRepository = WalletRepository(
                walletDao = database.walletDao(),
                orderDao = database.orderDao(),
                firestore = FirebaseFirestore.getInstance()
            )
            return TopUpViewModel(
                walletRepository = walletRepository,
                mpesaRepository = MpesaRepository()
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
} 