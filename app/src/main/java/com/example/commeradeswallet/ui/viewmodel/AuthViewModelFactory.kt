package com.example.commeradeswallet.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.commeradeswallet.data.repository.AuthRepository

class AuthViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            try {
                return AuthViewModel(
                    repository = AuthRepository()
                ) as T
            } catch (e: Exception) {
                throw RuntimeException("Error creating AuthViewModel: ${e.message}")
            }
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
} 