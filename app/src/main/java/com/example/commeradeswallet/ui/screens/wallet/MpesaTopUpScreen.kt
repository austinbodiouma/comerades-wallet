package com.example.commeradeswallet.ui.screens.wallet

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.commeradeswallet.ui.viewmodel.MpesaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MpesaTopUpScreen(
    onNavigateBack: () -> Unit,
    mpesaViewModel: MpesaViewModel = hiltViewModel()
) {
    var phoneNumber by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    
    val transactionState by mpesaViewModel.transactionState.collectAsState()
    
    LaunchedEffect(transactionState) {
        when (transactionState) {
            is MpesaViewModel.TransactionState.Loading -> isLoading = true
            is MpesaViewModel.TransactionState.Success -> {
                isLoading = false
                // Navigate back after successful transaction
                onNavigateBack()
            }
            is MpesaViewModel.TransactionState.Error -> isLoading = false
            else -> isLoading = false
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("M-Pesa Top Up", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Top Up Your Wallet",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { phoneNumber = it },
                label = { Text("Phone Number") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                placeholder = { Text("e.g., 254712345678") }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("Amount (KES)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = {
                    if (phoneNumber.isNotEmpty() && amount.isNotEmpty()) {
                        val amountValue = amount.toDoubleOrNull() ?: 0.0
                        if (amountValue > 0) {
                            mpesaViewModel.initiateStk(
                                phoneNumber = phoneNumber,
                                amount = amountValue.toString(),
                                paymentReason = "WALLET_TOPUP"
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !isLoading && phoneNumber.isNotEmpty() && amount.isNotEmpty()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Top Up")
                }
            }
            
            if (transactionState is MpesaViewModel.TransactionState.Error) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = (transactionState as MpesaViewModel.TransactionState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Note:",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• You will receive an M-Pesa prompt on your phone to complete the transaction.\n" +
                              "• Your wallet balance will be updated once payment is confirmed.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
} 