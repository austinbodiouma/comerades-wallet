package com.example.commeradeswallet.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.commeradeswallet.auth.GoogleAuthClient
import com.example.commeradeswallet.auth.SignInResult
import com.example.commeradeswallet.auth.UserData
import com.example.commeradeswallet.data.dao.UserDao
import com.example.commeradeswallet.data.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.security.MessageDigest

class AuthViewModel(
    private val googleAuthClient: GoogleAuthClient,
    private val userDao: UserDao
) : ViewModel() {

    private val _state = MutableStateFlow(AuthState())
    val state = _state.asStateFlow()

    init {
        // Check if user is already signed in
        viewModelScope.launch {
            try {
                val user = googleAuthClient.getSignedInUser()
                if (user != null) {
                    _state.update { 
                        it.copy(
                            isSignInSuccessful = true,
                            userData = user
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error checking initial auth state", e)
            }
        }
    }

    fun register(name: String, email: String, password: String) {
        viewModelScope.launch {
            try {
                _state.update { it.copy(isLoading = true) }
                
                // Check if user already exists
                val existingUser = userDao.getUserByEmail(email)
                if (existingUser != null) {
                    _state.update { 
                        it.copy(
                            isLoading = false,
                            signInError = "Email already registered"
                        )
                    }
                    return@launch
                }

                // Create new user
                val hashedPassword = hashPassword(password)
                val newUser = User(
                    email = email,
                    password = hashedPassword,
                    name = name,
                    authProvider = "EMAIL"
                )

                val userId = userDao.insertUser(newUser)
                
                Log.d("AuthViewModel", "User registered successfully with ID: $userId")
                
                _state.update { 
                    it.copy(
                        isLoading = false,
                        isSignInSuccessful = true,
                        userData = UserData(
                            userId = userId.toString(),
                            username = name,
                            email = email,
                            profilePictureUrl = null
                        )
                    )
                }
                
                Log.d("AuthViewModel", "State updated after registration: ${_state.value}")
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Registration failed", e)
                _state.update { 
                    it.copy(
                        isLoading = false,
                        signInError = e.message ?: "Registration failed"
                    )
                }
            }
        }
    }

    fun signInWithEmail(email: String, password: String) {
        viewModelScope.launch {
            try {
                _state.update { it.copy(isLoading = true) }
                
                val hashedPassword = hashPassword(password)
                val user = userDao.getUserByEmail(email)

                if (user != null && user.password == hashedPassword) {
                    _state.update { 
                        it.copy(
                            isLoading = false,
                            isSignInSuccessful = true,
                            userData = UserData(
                                userId = user.id.toString(),
                                username = user.name ?: "",
                                email = user.email,
                                profilePictureUrl = null
                            )
                        )
                    }
                } else {
                    _state.update { 
                        it.copy(
                            isLoading = false,
                            signInError = "Invalid email or password"
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Email sign in failed", e)
                _state.update { 
                    it.copy(
                        isLoading = false,
                        signInError = e.message ?: "Sign in failed"
                    )
                }
            }
        }
    }

    fun onSignInResult(result: SignInResult) {
        viewModelScope.launch {
            try {
                _state.update { it.copy(isLoading = true) }
                
                result.data?.let { userData ->
                    // Check if user exists in local DB
                    var user = userDao.getUserByGoogleId(userData.userId)
                    if (user == null) {
                        // Create new user
                        val newUser = User(
                            email = userData.email,
                            password = "", // No password for Google auth
                            name = userData.username,
                            authProvider = "GOOGLE",
                            googleUserId = userData.userId
                        )
                        userDao.insertUser(newUser)
                    }
                }
                
                _state.update { 
                    it.copy(
                        isLoading = false,
                        isSignInSuccessful = result.data != null,
                        userData = result.data,
                        signInError = result.errorMessage
                    )
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Google sign in failed", e)
                _state.update { 
                    it.copy(
                        isLoading = false,
                        signInError = e.message ?: "Sign in failed"
                    )
                }
            }
        }
    }

    private fun hashPassword(password: String): String {
        val bytes = password.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }

    fun resetState() {
        _state.update { AuthState() }
    }

    fun signOut() {
        viewModelScope.launch {
            try {
                googleAuthClient.signOut()
                _state.update { AuthState() }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Sign out failed", e)
                _state.update { it.copy(signInError = e.message) }
            }
        }
    }
}

data class AuthState(
    val isSignInSuccessful: Boolean = false,
    val signInError: String? = null,
    val isLoading: Boolean = false,
    val userData: UserData? = null
) 