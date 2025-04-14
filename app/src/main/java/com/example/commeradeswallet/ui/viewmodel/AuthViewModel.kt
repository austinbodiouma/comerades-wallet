package com.example.commeradeswallet.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.commeradeswallet.auth.GoogleAuthClient
import com.example.commeradeswallet.auth.SignInResult
import com.example.commeradeswallet.auth.UserData
import com.example.commeradeswallet.data.dao.UserDao
import com.example.commeradeswallet.data.model.User
import com.example.commeradeswallet.data.repository.AuthRepository
import com.example.commeradeswallet.util.FirestoreHelper
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.security.MessageDigest

class AuthViewModel(
    private val repository: AuthRepository
) : ViewModel() {
    private val TAG = "AuthViewModel"

    private val _authState = MutableStateFlow<AuthState>(AuthState.Initial)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _userProfile = MutableStateFlow<Map<String, Any>?>(null)
    val userProfile: StateFlow<Map<String, Any>?> = _userProfile.asStateFlow()

    // For compatibility with existing UI
    private val _state = MutableStateFlow(AuthState(isSignInSuccessful = false))
    val state = _state.asStateFlow()

    init {
        // Check if user is already signed in
        repository.currentUser?.let { user ->
            _authState.value = AuthState.Authenticated(user)
            loadUserProfile()
            _state.value = AuthState(isSignInSuccessful = true)
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            _state.value = _state.value.copy(isLoading = true)
            
            try {
                val result = repository.signIn(email, password)
                result.fold(
                    onSuccess = { user ->
                        _authState.value = AuthState.Authenticated(user)
                        _state.value = _state.value.copy(
                            isSignInSuccessful = true,
                            isLoading = false
                        )
                        
                        // Verify if the user profile exists in Firestore
                        verifyUserProfileExists()
                        
                        // Load the user profile
                        loadUserProfile()
                    },
                    onFailure = { e ->
                        val errorMessage = e.message ?: "Authentication failed"
                        _authState.value = AuthState.Error(errorMessage)
                        _state.value = _state.value.copy(
                            isLoading = false,
                            signInError = errorMessage
                        )
                        Log.e(TAG, "Sign in failed", e)
                    }
                )
            } catch (e: Exception) {
                val errorMessage = e.message ?: "Authentication failed"
                _authState.value = AuthState.Error(errorMessage)
                _state.value = _state.value.copy(
                    isLoading = false,
                    signInError = errorMessage
                )
                Log.e(TAG, "Sign in failed with exception", e)
            }
        }
    }

    fun signUp(
        email: String,
        password: String,
        name: String,
        studentId: String,
        phoneNumber: String
    ) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            _state.value = _state.value.copy(isLoading = true)
            
            try {
                Log.d(TAG, "Attempting to sign up user: $email")
                val result = repository.signUp(email, password, name, studentId, phoneNumber)
                result.fold(
                    onSuccess = { user ->
                        _authState.value = AuthState.Authenticated(user)
                        _state.value = _state.value.copy(
                            isSignInSuccessful = true,
                            isLoading = false
                        )
                        
                        Log.d(TAG, "User registration successful: ${user.uid}")
                        
                        // Verify if the user profile was created in Firestore
                        verifyUserProfileExists()
                        
                        // Load the user profile
                        loadUserProfile()
                    },
                    onFailure = { e ->
                        val errorMessage = e.message ?: "Registration failed"
                        _authState.value = AuthState.Error(errorMessage)
                        _state.value = _state.value.copy(
                            isLoading = false,
                            signInError = errorMessage
                        )
                        Log.e(TAG, "User registration failed", e)
                    }
                )
            } catch (e: Exception) {
                val errorMessage = e.message ?: "Registration failed"
                _authState.value = AuthState.Error(errorMessage)
                _state.value = _state.value.copy(
                    isLoading = false,
                    signInError = errorMessage
                )
                Log.e(TAG, "User registration failed with exception", e)
            }
        }
    }

    fun resetPassword(email: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            _state.value = _state.value.copy(isLoading = true)
            
            try {
                val result = repository.resetPassword(email)
                result.fold(
                    onSuccess = {
                        _authState.value = AuthState.ResetEmailSent
                        _state.value = _state.value.copy(isLoading = false)
                        Log.d(TAG, "Password reset email sent successfully")
                    },
                    onFailure = { e ->
                        val errorMessage = e.message ?: "Failed to send reset email"
                        _authState.value = AuthState.Error(errorMessage)
                        _state.value = _state.value.copy(
                            isLoading = false,
                            signInError = errorMessage
                        )
                        Log.e(TAG, "Failed to send password reset email", e)
                    }
                )
            } catch (e: Exception) {
                val errorMessage = e.message ?: "Failed to send reset email"
                _authState.value = AuthState.Error(errorMessage)
                _state.value = _state.value.copy(
                    isLoading = false,
                    signInError = errorMessage
                )
                Log.e(TAG, "Failed to send password reset email with exception", e)
            }
        }
    }

    fun signOut() {
        repository.signOut()
        _authState.value = AuthState.SignedOut
        _userProfile.value = null
        _state.value = AuthState(isSignInSuccessful = false)
        Log.d(TAG, "User signed out")
    }
    
    // For compatibility with existing code
    fun onSignInResult(result: SignInResult) {
        _state.value = _state.value.copy(
            isSignInSuccessful = result.data != null,
            signInError = result.errorMessage,
            userData = result.data
        )
        
        if (result.data != null) {
            viewModelScope.launch {
                verifyUserProfileExists()
            }
        }
    }
    
    // For compatibility with existing code
    fun resetState() {
        _state.value = AuthState()
    }
    
    // For compatibility with existing code
    fun register(name: String, email: String, password: String) {
        signUp(email, password, name, "", "") // Passing empty studentId and phoneNumber
    }
    
    // For compatibility with existing code
    fun signInWithEmail(email: String, password: String) {
        signIn(email, password)
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Loading user profile")
                val result = repository.getUserProfile()
                result.fold(
                    onSuccess = { profile ->
                        _userProfile.value = profile
                        Log.d(TAG, "User profile loaded successfully: $profile")
                    },
                    onFailure = { e ->
                        // Handle error but don't update auth state
                        // as user is still authenticated
                        Log.e(TAG, "Failed to load user profile", e)
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Exception while loading user profile", e)
            }
        }
    }
    
    private suspend fun verifyUserProfileExists() {
        try {
            Log.d(TAG, "Verifying user data in Firestore...")
            val hasUserData = FirestoreHelper.verifyUserData()
            Log.d(TAG, "User data verification result: $hasUserData")
            
            if (hasUserData) {
                val profileFields = FirestoreHelper.getUserProfileFields()
                Log.d(TAG, "User profile fields: $profileFields")
                
                val walletFields = FirestoreHelper.getUserWalletFields()
                Log.d(TAG, "User wallet fields: $walletFields")
                
                // Log wallet balance
                val balance = walletFields?.get("balance") as? Number
                Log.d(TAG, "User wallet balance: ${balance?.toDouble() ?: 0.0}")
            } else {
                Log.w(TAG, "User data incomplete in Firestore")
                
                // Check individual components
                val hasProfile = FirestoreHelper.verifyUserHasProfile()
                val hasWallet = FirestoreHelper.verifyUserHasWallet()
                
                Log.w(TAG, "Missing data: profile=${!hasProfile}, wallet=${!hasWallet}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to verify user data existence", e)
        }
    }

    sealed class AuthState {
        object Initial : AuthState()
        object Loading : AuthState()
        data class Authenticated(val user: FirebaseUser) : AuthState()
        data class Error(val message: String) : AuthState()
        object ResetEmailSent : AuthState()
        object SignedOut : AuthState()
    }

    class Factory(private val repository: AuthRepository = AuthRepository()) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AuthViewModel(repository) as T
        }
    }
}

data class AuthState(
    val isSignInSuccessful: Boolean = false,
    val signInError: String? = null,
    val isLoading: Boolean = false,
    val userData: UserData? = null
) 