package com.example.commeradeswallet.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.commeradeswallet.data.dao.WalletDao
import com.example.commeradeswallet.data.model.TransactionType
import com.example.commeradeswallet.data.model.WalletTransaction
import com.example.commeradeswallet.data.repository.MpesaRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MpesaViewModel(
    private val mpesaRepository: MpesaRepository,
    private val walletDao: WalletDao
) : ViewModel() {
    private val _transactionState = MutableStateFlow<TransactionState>(TransactionState.Idle)
    val transactionState: StateFlow<TransactionState> = _transactionState

    fun initiatePayment(phoneNumber: String, amount: Int, userId: String) {
        viewModelScope.launch {
            _transactionState.value = TransactionState.Processing

            try {
                mpesaRepository.initiateSTKPush(phoneNumber, amount)
                    .onSuccess { checkoutRequestId ->
                        // Save pending transaction
                        val transaction = WalletTransaction(
                            userId = userId,
                            amount = amount.toDouble(),
                            type = TransactionType.DEPOSIT,
                            description = "M-Pesa Top-up",
                            reference = checkoutRequestId
                        )
                        walletDao.insertTransaction(transaction)
                        
                        _transactionState.value = TransactionState.AwaitingConfirmation(checkoutRequestId)
                        startStatusCheck(checkoutRequestId)
                    }
                    .onFailure { error ->
                        _transactionState.value = TransactionState.Failed(error.message ?: "Payment failed")
                    }
            } catch (e: Exception) {
                _transactionState.value = TransactionState.Failed(e.message ?: "Payment failed")
            }
        }
    }

    private fun startStatusCheck(checkoutRequestId: String) {
        viewModelScope.launch {
            repeat(6) { attempt ->
                delay(10_000) // Wait 10 seconds between checks
                
                mpesaRepository.checkTransactionStatus(checkoutRequestId)
                    .onSuccess { success ->
                        if (success) {
                            walletDao.updateTransactionStatus(checkoutRequestId, "COMPLETED")
                            _transactionState.value = TransactionState.Completed
                            return@launch
                        } else if (attempt == 5) { // Last attempt
                            walletDao.updateTransactionStatus(checkoutRequestId, "FAILED")
                            _transactionState.value = TransactionState.Failed("Transaction timed out")
                        }
                    }
                    .onFailure { error ->
                        if (attempt == 5) { // Last attempt
                            walletDao.updateTransactionStatus(checkoutRequestId, "FAILED")
                            _transactionState.value = TransactionState.Failed(error.message ?: "Transaction failed")
                        }
                    }
            }
        }
    }
}

sealed class TransactionState {
    object Idle : TransactionState()
    object Processing : TransactionState()
    data class AwaitingConfirmation(val checkoutRequestId: String) : TransactionState()
    object Completed : TransactionState()
    data class Failed(val error: String) : TransactionState()
} 