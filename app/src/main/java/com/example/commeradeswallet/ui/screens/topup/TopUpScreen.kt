package com.example.commeradeswallet.ui.screens.topup

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.commeradeswallet.ui.preview.PreviewWrapper
import com.example.commeradeswallet.ui.preview.ThemePreviews
import com.example.commeradeswallet.util.format

@Composable
fun TopUpScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TopUpViewModel = viewModel(factory = TopUpViewModelFactory(LocalContext.current))
) {
    val state by viewModel.state.collectAsState()

    TopUpContent(
        amount = state.amount,
        phoneNumber = state.phoneNumber,
        isLoading = state.isLoading,
        error = state.error,
        success = state.success,
        currentBalance = state.currentBalance,
        onAmountChange = viewModel::onAmountChange,
        onPhoneNumberChange = viewModel::onPhoneNumberChange,
        onTopUp = viewModel::initiateTopUp,
        onRetry = viewModel::resetState,
        onNavigateBack = onNavigateBack,
        modifier = modifier
    )
}

@Composable
fun TopUpContent(
    amount: String,
    phoneNumber: String,
    isLoading: Boolean,
    error: String?,
    success: Boolean,
    currentBalance: Double,
    onAmountChange: (String) -> Unit,
    onPhoneNumberChange: (String) -> Unit,
    onTopUp: () -> Unit,
    onRetry: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Top Up Wallet",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(
            text = "Current Balance: KES ${currentBalance.format(2)}",
            fontSize = 16.sp,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        if (success) {
            SuccessScreen(onNavigateBack)
        } else if (error != null) {
            ErrorScreen(error, onRetry)
        } else {
            TopUpForm(
                amount = amount,
                phoneNumber = phoneNumber,
                isLoading = isLoading,
                onAmountChange = onAmountChange,
                onPhoneNumberChange = onPhoneNumberChange,
                onTopUp = onTopUp
            )
        }
    }
}

@Composable
fun TopUpForm(
    amount: String,
    phoneNumber: String,
    isLoading: Boolean,
    onAmountChange: (String) -> Unit,
    onPhoneNumberChange: (String) -> Unit,
    onTopUp: () -> Unit
) {
    Column {
        OutlinedTextField(
            value = amount,
            onValueChange = onAmountChange,
            label = { Text("Amount (KES)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = phoneNumber,
            onValueChange = onPhoneNumberChange,
            label = { Text("Phone Number") },
            placeholder = { Text("e.g. 254712345678") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onTopUp,
            enabled = !isLoading && amount.isNotEmpty() && phoneNumber.isNotEmpty(),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Top Up")
            }
        }
    }
}

@Composable
fun ErrorScreen(
    error: String,
    onRetry: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = "Error",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = error,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(onClick = onRetry) {
            Text("Try Again")
        }
    }
}

@Composable
fun SuccessScreen(
    onNavigateBack: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = "Top Up Initiated",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Your top up request has been initiated. Please check your phone for the M-Pesa payment prompt and enter your PIN to complete the transaction.",
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(onClick = onNavigateBack) {
            Text("Back to Wallet")
        }
    }
}

@ThemePreviews
@Composable
private fun TopUpScreenPreview() {
    PreviewWrapper {
        TopUpContent(
            amount = "500",
            phoneNumber = "254712345678",
            isLoading = false,
            error = null,
            success = false,
            currentBalance = 1250.0,
            onAmountChange = {},
            onPhoneNumberChange = {},
            onTopUp = {},
            onRetry = {},
            onNavigateBack = {}
        )
    }
}

@ThemePreviews
@Composable
private fun TopUpSuccessPreview() {
    PreviewWrapper {
        TopUpContent(
            amount = "500",
            phoneNumber = "254712345678",
            isLoading = false,
            error = null,
            success = true,
            currentBalance = 1250.0,
            onAmountChange = {},
            onPhoneNumberChange = {},
            onTopUp = {},
            onRetry = {},
            onNavigateBack = {}
        )
    }
}

@ThemePreviews
@Composable
private fun TopUpErrorPreview() {
    PreviewWrapper {
        TopUpContent(
            amount = "500",
            phoneNumber = "254712345678",
            isLoading = false,
            error = "Failed to initiate payment. Please try again later.",
            success = false,
            currentBalance = 1250.0,
            onAmountChange = {},
            onPhoneNumberChange = {},
            onTopUp = {},
            onRetry = {},
            onNavigateBack = {}
        )
    }
} 