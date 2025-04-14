package com.example.commeradeswallet.ui.screens.wallet

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.commeradeswallet.data.repository.MpesaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TopUpState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false
)

class TopUpViewModel(
    private val mpesaRepository: MpesaRepository
) : ViewModel() {
    private val _state = MutableStateFlow(TopUpState())
    val state: StateFlow<TopUpState> = _state.asStateFlow()

    fun initiateTopUp(phoneNumber: String, amount: Double) {
        viewModelScope.launch {
            try {
                _state.value = TopUpState(isLoading = true)
                
                // Convert amount to cents/smallest currency unit
                val amountInCents = (amount * 100).toInt()
                
                val result = mpesaRepository.initiateSTKPush(
                    phoneNumber = phoneNumber,
                    amount = amountInCents
                )
                
                result.fold(
                    onSuccess = { response ->
                        if (response.responseCode == "0") {
                            _state.value = TopUpState(success = true)
                        } else {
                            _state.value = TopUpState(
                                error = response.customerMessage ?: "Failed to initiate payment"
                            )
                        }
                    },
                    onFailure = { error ->
                        Log.e("TopUpViewModel", "Error initiating top up", error)
                        _state.value = TopUpState(error = error.message ?: "An error occurred")
                    }
                )
            } catch (e: Exception) {
                Log.e("TopUpViewModel", "Error initiating top up", e)
                _state.value = TopUpState(error = e.message ?: "An error occurred")
            }
        }
    }

    fun resetState() {
        _state.value = TopUpState()
    }
} 