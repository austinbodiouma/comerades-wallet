package com.example.commeradeswallet.ui.screens.auth

import android.content.Intent
import android.content.IntentSender
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.commeradeswallet.R
import com.example.commeradeswallet.auth.GoogleAuthClient
import com.example.commeradeswallet.ui.preview.PreviewWrapper
import com.example.commeradeswallet.ui.preview.ThemePreviews
import com.example.commeradeswallet.ui.viewmodel.AuthViewModel
import kotlinx.coroutines.launch
import androidx.lifecycle.viewmodel.compose.viewModel
import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.lifecycle.lifecycleScope
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.google.firebase.FirebaseApp
import com.example.commeradeswallet.CommeradesWalletApp
import kotlinx.coroutines.delay
import android.util.Log
import androidx.compose.material.icons.filled.Star
import com.example.commeradeswallet.data.AppDatabase
import com.example.commeradeswallet.ui.viewmodel.AuthViewModelFactory
import com.example.commeradeswallet.data.repository.AuthRepository

@Composable
fun AuthScreen(
    onNavigateToRegister: () -> Unit,
    onNavigateToHome: () -> Unit,
    viewModel: AuthViewModel = viewModel(factory = AuthViewModelFactory(LocalContext.current))
) {
    val context = LocalContext.current
    val googleAuthClient = remember { GoogleAuthClient(context) }
    var isInitializing by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    // Define launcher first
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
        onResult = { result ->
            Log.d("AuthScreen", "Received activity result: ${result.resultCode}")
            if (result.resultCode == Activity.RESULT_OK) {
                scope.launch {
                    try {
                        val signInResult = googleAuthClient.signInWithIntent(
                            intent = result.data ?: return@launch
                        )
                        Log.d("AuthScreen", "Sign in result: success=${signInResult.data != null}, error=${signInResult.errorMessage}")
                        viewModel.onSignInResult(signInResult)
                    } catch (e: Exception) {
                        Log.e("AuthScreen", "Error processing sign in result", e)
                    }
                }
            } else {
                Log.d("AuthScreen", "Sign in cancelled or failed: ${result.resultCode}")
            }
        }
    )

    // Then define the sign in function
    fun signIn() {
        scope.launch {
            try {
                val signInIntentSender = googleAuthClient.signIn()
                if (signInIntentSender == null) {
                    Log.e("AuthScreen", "Sign in intent sender is null")
                    return@launch
                }
                launcher.launch(
                    IntentSenderRequest.Builder(
                        signInIntentSender ?: return@launch
                    ).build()
                )
            } catch (e: Exception) {
                Log.e("AuthScreen", "Error launching sign in", e)
            }
        }
    }

    LaunchedEffect(Unit) {
        try {
            delay(1000)
            if (viewModel.authState.value is AuthViewModel.AuthState.Authenticated) {
                Log.d("AuthScreen", "User already signed in, navigating to home")
                onNavigateToHome()
            }
        } catch (e: Exception) {
            Log.e("AuthScreen", "Error checking auth state", e)
        } finally {
            isInitializing = false
        }
    }

    val authState by viewModel.authState.collectAsState()

    LaunchedEffect(authState) {
        when (authState) {
            is AuthViewModel.AuthState.Authenticated -> {
                Log.d("AuthScreen", "Authentication successful, navigating to home")
                onNavigateToHome()
            }
            is AuthViewModel.AuthState.Error -> {
                Log.e("AuthScreen", "Authentication error: ${(authState as AuthViewModel.AuthState.Error).message}")
            }
            else -> {}
        }
    }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }

    fun validateInput(email: String, password: String): Boolean {
        var isValid = true
        
        if (email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailError = "Please enter a valid email address"
            isValid = false
        }
        
        if (password.length < 6) {
            passwordError = "Password must be at least 6 characters"
            isValid = false
        }
        
        return isValid
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .systemBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        
        // Illustration
        Image(
            painter = painterResource(id = R.drawable.ic_login_illustration),
            contentDescription = null,
            modifier = Modifier
                .size(200.dp)
                .padding(bottom = 32.dp)
        )

        Text(
            text = "Hello",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        Text(
            text = "Welcome back, you've been missed!",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.padding(bottom = 32.dp)
        )

        OutlinedTextField(
            value = email,
            onValueChange = { 
                email = it
                emailError = null
            },
            label = { Text("Email") },
            isError = emailError != null,
            supportingText = emailError?.let { { Text(it) } },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            ),
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        )

        OutlinedTextField(
            value = password,
            onValueChange = { 
                password = it
                passwordError = null
            },
            label = { Text("Password") },
            visualTransformation = if (showPassword) 
                VisualTransformation.None 
            else 
                PasswordVisualTransformation(),
            isError = passwordError != null,
            supportingText = passwordError?.let { { Text(it) } },
            trailingIcon = {
                IconButton(onClick = { showPassword = !showPassword }) {
                    Icon(
                        imageVector = if (showPassword) 
                            Icons.Default.Star
                        else 
                            Icons.Default.Star,
                        contentDescription = if (showPassword) 
                            "Hide password" 
                        else 
                            "Show password"
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            ),
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        )

        Button(
            onClick = {
                if (validateInput(email, password)) {
                    viewModel.signIn(email, password)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .height(50.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (authState is AuthViewModel.AuthState.Loading) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Text("Login", fontSize = 16.sp)
            }
        }

        OutlinedButton(
            onClick = { signIn() },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_google),
                    contentDescription = "Google Icon",
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Continue with Google",
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier.padding(bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Don't have an account?",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            TextButton(onClick = onNavigateToRegister) {
                Text("Sign Up", color = MaterialTheme.colorScheme.primary)
            }
        }
    }

    // Show error dialog if needed
    if (authState is AuthViewModel.AuthState.Error) {
        val errorMessage = (authState as AuthViewModel.AuthState.Error).message
        AlertDialog(
            onDismissRequest = { /* Dismiss dialog */ },
            title = { Text("Error") },
            text = { Text(errorMessage) },
            confirmButton = {
                TextButton(onClick = { /* Dismiss dialog */ }) {
                    Text("OK")
                }
            }
        )
    }
}

@ThemePreviews
@Composable
private fun AuthScreenPreview() {
    PreviewWrapper {
        AuthScreen(
            onNavigateToRegister = {},
            onNavigateToHome = {}
        )
    }
}

