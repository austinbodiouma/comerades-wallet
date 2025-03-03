package com.example.commeradeswallet.ui.screens.wallet

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.commeradeswallet.data.AppDatabase
import com.example.commeradeswallet.data.model.WalletTransaction
import com.example.commeradeswallet.data.repository.WalletRepository
import com.example.commeradeswallet.ui.preview.PreviewWalletRepository
import kotlinx.coroutines.flow.*

class WalletViewModel(
    private val repository: WalletRepository
) : ViewModel() {
    val balance: StateFlow<Double> = repository.getBalance()
        .catch { emit(0.0) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val transactions: StateFlow<List<WalletTransaction>> = repository.getTransactions()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                try {
                    val application = checkNotNull(get(APPLICATION_KEY))
                    val database = AppDatabase.getDatabase(application)
                    val repository = WalletRepository(
                        walletDao = database.walletDao(),
                        orderDao = database.orderDao()
                    )
                    WalletViewModel(repository)
                } catch (e: Exception) {
                    Log.e("WalletViewModel", "Error creating ViewModel", e)
                    throw e
                }
            }
        }

        fun previewViewModel(): WalletViewModel {
            return WalletViewModel(PreviewWalletRepository())
        }
    }
} 