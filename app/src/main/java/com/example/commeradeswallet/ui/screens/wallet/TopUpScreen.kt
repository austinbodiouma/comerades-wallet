package com.example.commeradeswallet.ui.screens.wallet

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.commeradeswallet.BuildConfig
import com.example.commeradeswallet.ui.viewmodel.MpesaViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopUpScreen(
    onNavigateBack: () -> Unit,
    onNavigateToHistory: () -> Unit,
    viewModel: MpesaViewModel = createMpesaViewModel()
) {
    var phoneNumber by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var phoneError by remember { mutableStateOf<String?>(null) }
    
    val mpesaState by viewModel.state.collectAsState()
    val transactionState by viewModel.transactionState.collectAsState()

    // Process state changes
    LaunchedEffect(mpesaState) {
        when (mpesaState) {
            is MpesaViewModel.MpesaState.Success -> {
                // Wait for a few seconds before automatically checking status
                delay(5000)
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Top Up Wallet", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToHistory) {
                        Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Transaction History")
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Phone Number Input
            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { 
                    // Only allow digits and limit to 12 characters
                    if (it.length <= 12 && it.all { char -> char.isDigit() || char == '+' }) {
                        phoneNumber = it
                        phoneError = null
                    }
                },
                label = { Text("Phone Number") },
                placeholder = { Text("e.g., 0712345678") },
                leadingIcon = {
                    Icon(Icons.Default.Phone, contentDescription = null)
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                isError = phoneError != null,
                supportingText = phoneError?.let { { Text(it) } }
            )

            // Amount Input
            OutlinedTextField(
                value = amount,
                onValueChange = { 
                    // Only allow digits and decimal point
                    if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) {
                        amount = it
                    }
                },
                label = { Text("Amount (KES)") },
                placeholder = { Text("Enter amount") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val formattedPhone = formatPhoneNumber(phoneNumber)
                    if (!validatePhoneNumber(phoneNumber)) {
                        phoneError = "Please enter a valid Kenyan phone number"
                        return@Button
                    }
                    
                    val amountValue = amount.toDoubleOrNull()?.toInt() ?: return@Button
                    viewModel.initiateStk(formattedPhone, amountValue)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = phoneNumber.isNotEmpty() && amount.isNotEmpty() && 
                          mpesaState !is MpesaViewModel.MpesaState.Loading
            ) {
                if (mpesaState is MpesaViewModel.MpesaState.Loading) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text("Pay with M-Pesa")
                }
            }

            // Help text
            Text(
                text = "Enter your M-Pesa phone number starting with 07XX, 01XX, or 254",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )

            // Status messages
            when (mpesaState) {
                is MpesaViewModel.MpesaState.Error -> {
                    Text(
                        text = (mpesaState as MpesaViewModel.MpesaState.Error).message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                is MpesaViewModel.MpesaState.Success -> {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "STK push sent to your phone",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Please check your phone ($phoneNumber) and enter your M-Pesa PIN to complete the transaction.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            
                            // Show transaction state if available
                            when (transactionState) {
                                is MpesaViewModel.TransactionState.Success -> {
                                    Text(
                                        text = (transactionState as MpesaViewModel.TransactionState.Success).message,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                }
                                is MpesaViewModel.TransactionState.Failed -> {
                                    Text(
                                        text = (transactionState as MpesaViewModel.TransactionState.Failed).message,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                }
                                else -> {
                                    // Loading state or none
                                    if (transactionState !is MpesaViewModel.TransactionState.Idle) {
                                        LinearProgressIndicator(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Button(
                        onClick = {
                            viewModel.resetState()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Text("Make Another Payment")
                    }
                }
                else -> {}
            }
        }
    }
} 