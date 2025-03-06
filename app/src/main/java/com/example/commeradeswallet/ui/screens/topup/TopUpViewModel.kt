package com.example.commeradeswallet.ui.screens.topup

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.commeradeswallet.data.model.TransactionType
import com.example.commeradeswallet.data.repository.MpesaRepository
import com.example.commeradeswallet.data.repository.WalletRepository
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

data class TopUpState(
    val amount: String = "",
    val phoneNumber: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false,
    val currentBalance: Double = 0.0
)

class TopUpViewModel(
    private val walletRepository: WalletRepository,
    private val mpesaRepository: MpesaRepository
) : ViewModel() {

    private val _state = MutableStateFlow(TopUpState())
    val state: StateFlow<TopUpState> = _state.asStateFlow()

    private val userId = Firebase.auth.currentUser?.uid
        ?: throw IllegalStateException("User must be logged in")

    init {
        viewModelScope.launch {
            walletRepository.getWalletBalance(userId).collect { balance ->
                _state.update { it.copy(currentBalance = balance) }
            }
        }
    }

    fun onAmountChange(amount: String) {
        if (amount.isEmpty() || amount.matches(Regex("^\\d+(\\.\\d{0,2})?$"))) {
            _state.update { it.copy(amount = amount, error = null) }
        }
    }

    fun onPhoneNumberChange(phoneNumber: String) {
        if (phoneNumber.matches(Regex("^[0-9]*$")) && phoneNumber.length <= 12) {
            _state.update { it.copy(phoneNumber = phoneNumber, error = null) }
        }
    }

    fun initiateTopUp() {
        viewModelScope.launch {
            try {
                _state.update { it.copy(isLoading = true, error = null) }

                val amount = _state.value.amount.toDoubleOrNull()
                    ?: throw IllegalArgumentException("Invalid amount")
                
                if (amount < 1) {
                    throw IllegalArgumentException("Amount must be at least 1")
                }

                val phoneNumber = formatPhoneNumber(_state.value.phoneNumber)
                    ?: throw IllegalArgumentException("Invalid phone number")

                val timestamp = SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault())
                    .format(System.currentTimeMillis())
                val txnReference = "CW$timestamp"

                val stkResult = mpesaRepository.initiateSTKPush(
                    phoneNumber = phoneNumber,
                    amount = amount.toInt(), 
                    reference = txnReference
                )

                if (stkResult.isSuccess) {
                    // Wait for callback from M-Pesa
                    _state.update { it.copy(
                        isLoading = false,
                        success = true,
                        error = null
                    )}

                    // Process the transaction in both Room and Firestore
                    walletRepository.processTransaction(
                        userId = userId,
                        amount = amount,
                        type = TransactionType.DEPOSIT,
                        description = "M-Pesa Top Up",
                        reference = txnReference
                    ).onFailure { e ->
                        _state.update { it.copy(
                            error = "Transaction failed: ${e.message}",
                            isLoading = false
                        )}
                    }
                } else {
                    throw stkResult.exceptionOrNull() 
                        ?: Exception("Failed to initiate M-Pesa payment")
                }
            } catch (e: Exception) {
                Log.e("TopUpViewModel", "Top up failed", e)
                _state.update { it.copy(
                    error = e.message ?: "An unknown error occurred",
                    isLoading = false
                )}
            }
        }
    }

    fun resetState() {
        _state.update { TopUpState(currentBalance = it.currentBalance) }
    }

    private fun formatPhoneNumber(phone: String): String? {
        return when {
            phone.matches(Regex("^254\\d{9}$")) -> phone
            phone.matches(Regex("^0\\d{9}$")) -> "254${phone.substring(1)}"
            phone.matches(Regex("^\\+254\\d{9}$")) -> phone.substring(1)
            else -> null
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