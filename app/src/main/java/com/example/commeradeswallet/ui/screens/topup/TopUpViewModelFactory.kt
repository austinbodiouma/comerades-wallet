package com.example.commeradeswallet.ui.screens.topup

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.commeradeswallet.data.AppDatabase
import com.example.commeradeswallet.data.repository.MpesaRepository
import com.example.commeradeswallet.ui.viewmodel.MpesaViewModel

class TopUpViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TopUpViewModel::class.java)) {
            val database = AppDatabase.getDatabase(context)
            val mpesaViewModel = MpesaViewModel(
                MpesaRepository(),
                database.walletDao()
            )
            return TopUpViewModel(
                repository = MpesaRepository(),
                mpesaViewModel = mpesaViewModel,
                walletDao = database.walletDao()
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
} 