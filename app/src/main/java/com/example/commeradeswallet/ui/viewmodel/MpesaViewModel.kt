package com.example.commeradeswallet.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.commeradeswallet.data.mpesa.models.MpesaTransaction
import com.example.commeradeswallet.data.mpesa.models.STKPushResponse
import com.example.commeradeswallet.data.repository.MpesaRepository
import com.example.commeradeswallet.data.repository.MpesaTransactionRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

class MpesaViewModel(
    private val mpesaRepository: MpesaRepository,
    private val transactionRepository: MpesaTransactionRepository,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    private val _state = MutableStateFlow<MpesaState>(MpesaState.Idle)
    val state: StateFlow<MpesaState> = _state.asStateFlow()

    private val _transactionState = MutableStateFlow<TransactionState>(TransactionState.Idle)
    val transactionState: StateFlow<TransactionState> = _transactionState.asStateFlow()

    private var currentPhoneNumber: String = ""
    private var currentAmount: Int = 0

    val transactions = flow {
        auth.currentUser?.let { user ->
            emitAll(transactionRepository.getTransactionsForUser(user.uid))
        }
    }

    fun initiateStk(phoneNumber: String, amount: Int) {
        viewModelScope.launch {
            try {
                _state.value = MpesaState.Loading

                // Check if user is logged in
                val currentUser = auth.currentUser
                if (currentUser == null) {
                    _state.value = MpesaState.Error("Please log in to continue")
                    return@launch
                }

                currentPhoneNumber = phoneNumber
                currentAmount = amount

                val result = mpesaRepository.initiateSTKPush(
                    phoneNumber = phoneNumber,
                    amount = amount
                )

                result.fold(
                    onSuccess = { response ->
                        Log.d("MpesaViewModel", "STK push successful: $response")
                        handleSuccessfulStkPush(response)
                        _state.value = MpesaState.Success(response)
                    },
                    onFailure = { error ->
                        Log.e("MpesaViewModel", "STK push failed", error)
                        _state.value = MpesaState.Error(error.message ?: "Failed to initiate payment")
                    }
                )
            } catch (e: Exception) {
                Log.e("MpesaViewModel", "Exception during STK push", e)
                _state.value = MpesaState.Error(e.message ?: "An unexpected error occurred")
            }
        }
    }

    private fun handleSuccessfulStkPush(response: STKPushResponse) {
        viewModelScope.launch {
            try {
                val currentUser = auth.currentUser
                if (currentUser == null) {
                    _state.value = MpesaState.Error("User not logged in")
                    return@launch
                }

                // Create transaction record
                transactionRepository.createTransaction(
                    userId = currentUser.uid,
                    phoneNumber = currentPhoneNumber,
                    amount = currentAmount.toDouble(),
                    merchantRequestId = response.merchantRequestID,
                    checkoutRequestId = response.checkoutRequestID
                )

                // Start polling for transaction status
                pollTransactionStatus(response.checkoutRequestID)
            } catch (e: Exception) {
                _state.value = MpesaState.Error("Failed to create transaction record: ${e.message}")
            }
        }
    }

    private fun pollTransactionStatus(checkoutRequestId: String) {
        viewModelScope.launch {
            var attempts = 0
            val maxAttempts = 10
            val delayBetweenPolls = 5000L // 5 seconds

            while (attempts < maxAttempts) {
                try {
                    val result = mpesaRepository.querySTKStatus(checkoutRequestId)
                    result.fold(
                        onSuccess = { response ->
                            // Update transaction status in Firestore
                            auth.currentUser?.let { user ->
                                transactionRepository.updateTransactionStatus(
                                    userId = user.uid,
                                    checkoutRequestId = checkoutRequestId,
                                    status = when {
                                        response.resultCode == "0" -> "COMPLETED"
                                        response.resultCode == null -> "PENDING"
                                        else -> "FAILED"
                                    },
                                    resultCode = response.resultCode,
                                    resultDesc = response.resultDesc
                                )
                            }

                            when (response.resultCode) {
                                "0" -> {
                                    _transactionState.value = TransactionState.Success("Payment completed successfully")
                                    return@launch // Exit polling on success
                                }
                                null -> {
                                    // Transaction still pending, continue polling
                                    attempts++
                                    delay(delayBetweenPolls)
                                }
                                else -> {
                                    _transactionState.value = TransactionState.Failed(response.resultDesc ?: "Payment failed")
                                    return@launch // Exit polling on failure
                                }
                            }
                        },
                        onFailure = { error ->
                            attempts++
                            if (attempts >= maxAttempts) {
                                _transactionState.value = TransactionState.Failed("Failed to check payment status after multiple attempts")
                                // Update Firestore with timeout status
                                auth.currentUser?.let { user ->
                                    transactionRepository.updateTransactionStatus(
                                        userId = user.uid,
                                        checkoutRequestId = checkoutRequestId,
                                        status = "TIMEOUT",
                                        resultCode = null,
                                        resultDesc = "Transaction status check timed out"
                                    )
                                }
                            } else {
                                delay(delayBetweenPolls)
                            }
                        }
                    )
                } catch (e: Exception) {
                    attempts++
                    if (attempts >= maxAttempts) {
                        _transactionState.value = TransactionState.Failed("Error checking payment status: ${e.message}")
                        break
                    }
                    delay(delayBetweenPolls)
                }
            }
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
    }

    class Factory(
        private val mpesaRepository: MpesaRepository,
        private val transactionRepository: MpesaTransactionRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MpesaViewModel(mpesaRepository, transactionRepository) as T
        }
    }
} 