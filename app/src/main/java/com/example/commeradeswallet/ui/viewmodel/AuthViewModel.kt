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

    fun onSignInResult(result: SignInResult) {
        viewModelScope.launch {
            try {
                result.data?.let { userData ->
                    // Check if user exists in local DB
                    val user = userDao.getUserByGoogleId(userData.userId)
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
                
                _state.update { it.copy(
                    isSignInSuccessful = result.data != null,
                    signInError = result.errorMessage
                ) }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error processing sign in", e)
                _state.update { it.copy(
                    isSignInSuccessful = false,
                    signInError = e.message
                ) }
            }
        }
    }

    fun signInWithEmail(email: String, password: String) {
        viewModelScope.launch {
            try {
                val hashedPassword = hashPassword(password)
                val user = userDao.getUserByEmail(email)

                if (user != null && user.password == hashedPassword) {
                    _state.update { it.copy(
                        isSignInSuccessful = true,
                        signInError = null
                    ) }
                } else {
                    _state.update { it.copy(
                        isSignInSuccessful = false,
                        signInError = "Invalid email or password"
                    ) }
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error signing in with email", e)
                _state.update { it.copy(
                    isSignInSuccessful = false,
                    signInError = e.message
                ) }
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
    val isLoading: Boolean = false
) 