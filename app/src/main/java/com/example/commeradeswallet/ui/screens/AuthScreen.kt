package com.example.commeradeswallet.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.commeradeswallet.ui.viewmodel.AuthViewModel
import kotlinx.coroutines.flow.collectLatest

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    viewModel: AuthViewModel = viewModel(factory = AuthViewModel.Factory())
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var studentId by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var isSignUp by remember { mutableStateOf(false) }
    var showResetPassword by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.authState.collectLatest { state ->
            when (state) {
                is AuthViewModel.AuthState.Authenticated -> {
                    onAuthSuccess()
                }
                is AuthViewModel.AuthState.Error -> {
                    // Show error message
                }
                else -> {}
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (showResetPassword) "Reset Password"
            else if (isSignUp) "Sign Up"
            else "Sign In",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (!showResetPassword && isSignUp) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Next
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = studentId,
                onValueChange = { studentId = it },
                label = { Text("Student ID") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Next
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { phoneNumber = it },
                label = { Text("Phone Number") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Next
                )
            )

            Spacer(modifier = Modifier.height(8.dp))
        }

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (!showResetPassword) {
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                )
            )

            Spacer(modifier = Modifier.height(8.dp))
        }

        Button(
            onClick = {
                when {
                    showResetPassword -> viewModel.resetPassword(email)
                    isSignUp -> viewModel.signUp(email, password, name, studentId, phoneNumber)
                    else -> viewModel.signIn(email, password)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = when {
                    showResetPassword -> "Send Reset Link"
                    isSignUp -> "Sign Up"
                    else -> "Sign In"
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (!showResetPassword) {
            TextButton(
                onClick = { isSignUp = !isSignUp }
            ) {
                Text(
                    text = if (isSignUp) "Already have an account? Sign In"
                    else "Don't have an account? Sign Up"
                )
            }

            TextButton(
                onClick = { showResetPassword = true }
            ) {
                Text("Forgot Password?")
            }
        } else {
            TextButton(
                onClick = { showResetPassword = false }
            ) {
                Text("Back to Sign In")
            }
        }
    }
} 