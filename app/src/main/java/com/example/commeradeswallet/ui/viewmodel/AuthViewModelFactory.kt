package com.example.commeradeswallet.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.commeradeswallet.auth.GoogleAuthClient
import com.example.commeradeswallet.data.AppDatabase

class AuthViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            val database = AppDatabase.getDatabase(context)
            return AuthViewModel(
                googleAuthClient = GoogleAuthClient(context),
                userDao = database.userDao()
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
} 