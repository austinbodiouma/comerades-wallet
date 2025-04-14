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
import com.example.commeradeswallet.data.mpesa.DarajaClient
import com.example.commeradeswallet.data.mpesa.DarajaRepository
import com.example.commeradeswallet.data.repository.MpesaRepository
import com.example.commeradeswallet.data.repository.MpesaTransactionRepository
import com.example.commeradeswallet.ui.viewmodel.MpesaViewModel
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

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

    // Function to format phone number to the required format
    fun formatPhoneNumber(number: String): String {
        // Remove any spaces, dashes, or other characters
        val cleaned = number.replace(Regex("[^0-9]"), "")
        
        return when {
            // If starts with 254, use as is
            cleaned.startsWith("254") -> cleaned
            // If starts with 0, replace with 254
            cleaned.startsWith("0") -> "254${cleaned.substring(1)}"
            // If starts with 7 or 1, add 254
            cleaned.startsWith("7") || cleaned.startsWith("1") -> "254$cleaned"
            // Otherwise return as is
            else -> cleaned
        }
    }

    // Function to validate phone number
    fun validatePhoneNumber(number: String): Boolean {
        val formatted = formatPhoneNumber(number)
        return formatted.length == 12 && formatted.startsWith("254")
    }

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

@Composable
private fun createMpesaViewModel(): MpesaViewModel {
    val context = LocalContext.current
    
    // Create HTTP client
    val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    
    val httpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
    
    // Create Retrofit API client
    val retrofit = Retrofit.Builder()
        .baseUrl("https://sandbox.safaricom.co.ke/")
        .client(httpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    val darajaClient = retrofit.create(DarajaClient::class.java)
    
    // Create repositories
    val darajaRepository = DarajaRepository(
        client = darajaClient,
        consumerKey = "i0Ci7KCr11HyeGDaVYfPbGE7ZcoYiyxsES4SlBabyCFgHGf3",
        consumerSecret = "wvQsxgvlaFvvJu7sGw2rzIIiWXC5GwSIwMpSq7VWRRwkYx0kbs05OGhsTR2C3Wc7",
        passKey = "MTc0Mzc5YmZiMjc5ZjlhYTliZGJjZjE1OGU5N2RkNzFhNDY3Y2QyZTBjODkzMDU5YjEwZjc4ZTZiNzJhZGExZWQyYzkxOTIwMjUwNDA2MDkyOTQ3",
        businessShortCode = "174379",
        callbackUrl = "https://mydomain.com/path"
    )
    
    val mpesaRepository = MpesaRepository(darajaRepository)
    val transactionRepository = MpesaTransactionRepository()
    
    return viewModel(
        factory = MpesaViewModel.Factory(mpesaRepository, transactionRepository)
    )
} 