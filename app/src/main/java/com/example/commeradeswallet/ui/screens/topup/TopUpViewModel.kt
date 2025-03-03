package com.example.commeradeswallet.ui.screens.topup

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import com.example.commeradeswallet.data.AppDatabase
import com.example.commeradeswallet.data.dao.WalletDao
import com.example.commeradeswallet.data.model.TransactionStatus
import com.example.commeradeswallet.data.model.TransactionType
import com.example.commeradeswallet.data.model.WalletTransaction
import com.example.commeradeswallet.data.repository.MpesaRepository
import com.example.commeradeswallet.ui.viewmodel.MpesaViewModel
import com.example.commeradeswallet.ui.viewmodel.TransactionState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

open class TopUpViewModel(
    private val repository: MpesaRepository = MpesaRepository(),
    private val mpesaViewModel: MpesaViewModel,
    private val walletDao: WalletDao
) : ViewModel() {

    protected val _uiState = MutableStateFlow<TopUpUiState>(TopUpUiState.Initial)
    val uiState: StateFlow<TopUpUiState> = _uiState

    val transactionState = mpesaViewModel.transactionState.map { state ->
        when (state) {
            is TransactionState.Idle -> TopUpUiState.Initial
            is TransactionState.Processing -> TopUpUiState.Loading
            is TransactionState.AwaitingConfirmation -> 
                TopUpUiState.PushSuccessful(state.checkoutRequestId)
            is TransactionState.Completed -> 
                TopUpUiState.TransactionComplete(true, "Top-up successful!")
            is TransactionState.Failed -> 
                TopUpUiState.Error(state.error)
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        TopUpUiState.Initial
    )

    fun initiateTopUp(phoneNumber: String, amount: String) {
        if (!validateInput(phoneNumber, amount)) return
        mpesaViewModel.initiatePayment(formatPhoneNumber(phoneNumber), amount.toInt())
    }

    private fun validateInput(phoneNumber: String, amount: String): Boolean {
        if (phoneNumber.isEmpty() || amount.isEmpty()) {
            _uiState.value = TopUpUiState.Error("Please fill in all fields")
            return false
        }

        if (!phoneNumber.matches(Regex("^(254|0|\\+254)7[0-9]{8}$"))) {
            _uiState.value = TopUpUiState.Error("Invalid phone number format")
            return false
        }

        try {
            val amountInt = amount.toInt()
            if (amountInt < 1) {
                _uiState.value = TopUpUiState.Error("Amount must be at least 1 KES")
                return false
            }
        } catch (e: NumberFormatException) {
            _uiState.value = TopUpUiState.Error("Invalid amount")
            return false
        }

        return true
    }

    private fun formatPhoneNumber(phone: String): String {
        return when {
            phone.startsWith("+254") -> phone.substring(1)
            phone.startsWith("0") -> "254${phone.substring(1)}"
            else -> phone
        }
    }

    private fun startCheckingStatus(checkoutRequestId: String) {
        viewModelScope.launch {
            // Wait for 10 seconds before first check
            delay(10_000)
            
            var attempts = 0
            val maxAttempts = 6  // Check for 1 minute (6 attempts * 10 seconds)
            
            while (attempts < maxAttempts) {
                repository.checkTransactionStatus(checkoutRequestId)
                    .onSuccess { success ->
                        if (success) {
                            _uiState.value = TopUpUiState.TransactionComplete(
                                success = true,
                                message = "Top-up successful!"
                            )
                            return@launch
                        } else {
                            // If explicitly failed (cancelled/timeout), stop checking
                            _uiState.value = TopUpUiState.TransactionComplete(
                                success = false,
                                message = "Transaction failed or was cancelled"
                            )
                            return@launch
                        }
                    }
                    .onFailure { error ->
                        // Only update state on last attempt
                        if (attempts == maxAttempts - 1) {
                            _uiState.value = TopUpUiState.Error(
                                error.message ?: "Failed to check transaction status"
                            )
                        }
                    }
                
                attempts++
                if (attempts < maxAttempts) {
                    delay(10_000)  // Wait 10 seconds between checks
                }
            }
            
            // If we reach here, transaction is pending
            _uiState.value = TopUpUiState.TransactionComplete(
                success = false,
                message = "Transaction is pending. Check your M-Pesa messages."
            )
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return when (modelClass) {
                    TopUpViewModel::class.java -> {
                        // Create a mock version for previews
                        if (android.os.Build.TYPE == "user") {
                            // Real implementation will be provided by DI in production
                            val mockWalletDao = object : WalletDao {
                                override fun getBalance(): Flow<Double> = flowOf(0.0)
                                override suspend fun getCurrentBalance(): Double = 0.0
                                override fun getTransactions(): Flow<List<WalletTransaction>> = flowOf(emptyList())
                                override suspend fun getTransactionByReference(mpesaReference: String): WalletTransaction? = null
                                override suspend fun insertTransaction(transaction: WalletTransaction): Long = 0L
                                override suspend fun updateTransaction(transaction: WalletTransaction) {}
                                override suspend fun updateTransactionStatus(reference: String, status: TransactionStatus) {}
                            }

                            val mockMpesaViewModel = MpesaViewModel(
                                MpesaRepository(),
                                mockWalletDao
                            )

                            TopUpViewModel(
                                repository = MpesaRepository(),
                                mpesaViewModel = mockMpesaViewModel,
                                walletDao = mockWalletDao
                            ) as T
                        } else {
                            throw IllegalStateException("Application context should be provided through DI")
                        }
                    }
                    else -> throw IllegalArgumentException("Unknown ViewModel class")
                }
            }
        }
    }
}

sealed class TopUpUiState {
    object Initial : TopUpUiState()
    object Loading : TopUpUiState()
    data class PushSuccessful(val checkoutRequestId: String) : TopUpUiState()
    data class Error(val message: String) : TopUpUiState()
    data class TransactionComplete(val success: Boolean, val message: String) : TopUpUiState()
} 