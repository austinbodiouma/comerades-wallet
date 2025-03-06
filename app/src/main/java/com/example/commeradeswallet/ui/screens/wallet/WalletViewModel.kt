package com.example.commeradeswallet.ui.screens.wallet

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.commeradeswallet.data.AppDatabase
import com.example.commeradeswallet.data.model.WalletTransaction
import com.example.commeradeswallet.data.repository.WalletRepository
import com.example.commeradeswallet.ui.preview.getPreviewWalletRepository
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class WalletViewModel(
    private val repository: WalletRepository
) : ViewModel() {
    private val userId = Firebase.auth.currentUser?.uid ?: ""
    
    private val _balance = MutableStateFlow(0.0)
    val balance: StateFlow<Double> = _balance.asStateFlow()
    
    private val _transactions = MutableStateFlow<List<WalletTransaction>>(emptyList())
    val transactions: StateFlow<List<WalletTransaction>> = _transactions.asStateFlow()
    
    init {
        if (userId.isNotEmpty()) {
            loadWalletData()
        }
    }
    
    private fun loadWalletData() {
        viewModelScope.launch {
            // Load balance
            repository.getWalletBalance(userId).collect { balance ->
                _balance.value = balance
            }
        }
        
        viewModelScope.launch {
            // Load transactions
            repository.getTransactions(userId).collect { transactions ->
                _transactions.value = transactions
            }
        }
    }
    
    fun syncWithFirestore() {
        if (userId.isEmpty()) return
        
        viewModelScope.launch {
            try {
                repository.syncWithFirestore(userId)
            } catch (e: Exception) {
                Log.e("WalletViewModel", "Error syncing with Firestore", e)
            }
        }
    }
    
    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(WalletViewModel::class.java)) {
                val database = AppDatabase.getDatabase(application)
                return WalletViewModel(
                    WalletRepository(
                        walletDao = database.walletDao(),
                        orderDao = database.orderDao(),
                        firestore = FirebaseFirestore.getInstance()
                    )
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
    
    companion object {
        fun previewViewModel(): WalletViewModel {
            return WalletViewModel(getPreviewWalletRepository())
        }
    }
} 