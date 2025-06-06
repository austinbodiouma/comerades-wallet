package com.example.commeradeswallet.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.commeradeswallet.data.model.User
import com.example.commeradeswallet.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class UserViewModel(
    private val repository: UserRepository = UserRepository()
) : ViewModel() {
    
    private val _userState = MutableStateFlow<User?>(null)
    val userState: StateFlow<User?> = _userState.asStateFlow()

    fun loadUser(userId: String) {
        viewModelScope.launch {
            repository.getUser(userId)
                .onSuccess { user ->
                    _userState.value = user
                }
                .onFailure {
                    // Handle error
                }
        }
    }
} 