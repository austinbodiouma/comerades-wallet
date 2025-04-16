package com.example.commeradeswallet.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.commeradeswallet.data.model.TransactionType
import com.example.commeradeswallet.data.mpesa.models.MpesaTransaction
import com.example.commeradeswallet.data.mpesa.models.STKPushResponse
import com.example.commeradeswallet.data.repository.MpesaRepository
import com.example.commeradeswallet.data.repository.MpesaTransactionRepository
import com.example.commeradeswallet.data.repository.WalletRepository
import com.google.firebase.auth.FirebaseAuth
import com.example.commeradeswallet.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import android.util.Log
import javax.inject.Inject

@HiltViewModel
class MpesaViewModel @Inject constructor(
    private val mpesaRepository: MpesaRepository,
    private val transactionRepository: MpesaTransactionRepository,
    private val walletRepository: WalletRepository,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    private val _state = MutableStateFlow<MpesaState>(MpesaState.Idle)
    val state: StateFlow<MpesaState> = _state.asStateFlow()

    private val _transactionState = MutableStateFlow<TransactionState>(TransactionState.Idle)
    val transactionState: StateFlow<TransactionState> = _transactionState.asStateFlow()

    private var currentPhoneNumber: String = ""
    private var currentAmount: Int = 0
    private var isTopUpRequest: Boolean = false

    val transactions = flow {
        auth.currentUser?.let { user ->
            emitAll(transactionRepository.getTransactionsForUser(user.uid))
        }
    }

    fun initiateStk(phoneNumber: String, amount: String, paymentReason: String) {
        viewModelScope.launch {
            _transactionState.value = TransactionState.Loading
            
            try {
                val result = mpesaRepository.initiateStk(phoneNumber, amount, paymentReason)
                
                when (result) {
                    is Resource.Success -> {
                        val responseCode = result.data?.responseCode
                        
                        if (responseCode == "0") {
                            // Transaction initiated successfully
                            _transactionState.value = TransactionState.Success("STK push sent successfully")
                            
                            // Don't update wallet balance here - it's premature
                            // The balance should only be updated after the transaction is confirmed
                            // updateWalletBalance(amount.toDouble())
                            
                            // Store the transaction info for later reference
                            currentPhoneNumber = phoneNumber
                            currentAmount = amount.toInt()
                            isTopUpRequest = paymentReason == "WALLET_TOPUP"
                            
                            // Create transaction record with wallet top-up flag
                            result.data?.let { response ->
                                try {
                                    transactionRepository.createTransaction(
                                        userId = auth.currentUser?.uid ?: "",
                                        phoneNumber = phoneNumber,
                                        amount = amount.toDouble(),
                                        merchantRequestId = response.merchantRequestID,
                                        checkoutRequestId = response.checkoutRequestID,
                                        isWalletTopUp = paymentReason == "WALLET_TOPUP"
                                    )
                                } catch (e: Exception) {
                                    Log.e("MpesaViewModel", "Error creating transaction record", e)
                                }
                            }
                        } else {
                            _transactionState.value = TransactionState.Error(
                                result.data?.responseDescription ?: "Failed to initiate transaction"
                            )
                        }
                    }
                    is Resource.Error -> {
                        _transactionState.value = TransactionState.Error(
                            result.message ?: "An unknown error occurred"
                        )
                    }
                    is Resource.Loading -> {
                        _transactionState.value = TransactionState.Loading
                    }
                }
            } catch (e: Exception) {
                Log.e("MpesaViewModel", "Error initiating M-Pesa transaction", e)
                _transactionState.value = TransactionState.Error("Failed to process payment: ${e.message}")
            }
        }
    }

    fun checkTransactionStatus(checkoutRequestId: String) {
        viewModelScope.launch {
            _transactionState.value = TransactionState.Loading
            try {
                val transaction = transactionRepository.getTransactionByCheckoutRequestId(checkoutRequestId)
                
                if (transaction != null) {
                    // If transaction is already completed and is a wallet top-up
                    if (transaction.status == "COMPLETED" && transaction.isWalletTopUp) {
                        _transactionState.value = TransactionState.Success("Transaction already completed")
                        return@launch
                    }
                    
                    // For transactions still pending, query M-Pesa for status
                    val queryResult = mpesaRepository.queryTransactionStatus(checkoutRequestId)
                    
                    if (queryResult is Resource.Success && queryResult.data?.resultCode == "0") {
                        // Update transaction status to COMPLETED
                        transactionRepository.updateTransactionStatus(
                            userId = transaction.userId,
                            checkoutRequestId = checkoutRequestId,
                            status = "COMPLETED",
                            resultCode = queryResult.data.resultCode,
                            resultDesc = queryResult.data.resultDesc
                        )
                        
                        // If this is a wallet top-up, update balance
                        if (transaction.isWalletTopUp) {
                            updateWalletBalance(transaction.amount)
                            _transactionState.value = TransactionState.Success("Wallet topped up successfully")
                        } else {
                            _transactionState.value = TransactionState.Success("Transaction completed successfully")
                        }
                    } else {
                        _transactionState.value = TransactionState.Failed(
                            queryResult?.message ?: "Failed to query transaction status"
                        )
                    }
                } else {
                    _transactionState.value = TransactionState.Failed("Transaction not found")
                }
            } catch (e: Exception) {
                Log.e("MpesaViewModel", "Error checking transaction status", e)
                _transactionState.value = TransactionState.Failed(e.localizedMessage ?: "An unknown error occurred")
            }
        }
    }
    
    private suspend fun updateWalletBalance(amount: Double) {
        try {
            walletRepository.addFunds(amount)
            Log.d("MpesaViewModel", "Wallet balance updated successfully with amount: $amount")
        } catch (e: Exception) {
            Log.e("MpesaViewModel", "Error updating wallet balance", e)
        }
    }

    fun resetState() {
        _state.value = MpesaState.Idle
        _transactionState.value = TransactionState.Idle
    }

    sealed class MpesaState {
        object Idle : MpesaState()
        object Loading : MpesaState()
        data class Success(val response: STKPushResponse) : MpesaState()
        data class Error(val message: String) : MpesaState()
    }

    sealed class TransactionState {
        object Idle : TransactionState()
        data class Success(val message: String) : TransactionState()
        data class Failed(val message: String) : TransactionState()
        object Loading : TransactionState()
    }

    class Factory(
        private val mpesaRepository: MpesaRepository,
        private val transactionRepository: MpesaTransactionRepository,
        private val walletRepository: WalletRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MpesaViewModel::class.java)) {
                return MpesaViewModel(mpesaRepository, transactionRepository, walletRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
} 