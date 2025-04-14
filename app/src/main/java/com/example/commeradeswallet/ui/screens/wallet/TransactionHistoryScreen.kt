package com.example.commeradeswallet.ui.screens.wallet

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.commeradeswallet.data.mpesa.models.MpesaTransaction
import com.example.commeradeswallet.data.mpesa.models.TransactionStatus
import com.example.commeradeswallet.data.repository.MpesaRepository
import com.example.commeradeswallet.data.repository.MpesaTransactionRepository
import com.example.commeradeswallet.ui.viewmodel.MpesaViewModel
import java.text.SimpleDateFormat
import java.util.*
import com.example.commeradeswallet.data.mpesa.DarajaClient
import com.example.commeradeswallet.data.mpesa.DarajaRepository
import com.example.commeradeswallet.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionHistoryScreen(
    onNavigateBack: () -> Unit,
    viewModel: MpesaViewModel = viewModel(
        factory = MpesaViewModel.Factory(
            MpesaRepository(
                DarajaRepository(
                    client = createDarajaClient(),
                    consumerKey = "i0Ci7KCr11HyeGDaVYfPbGE7ZcoYiyxsES4SlBabyCFgHGf3",
                    consumerSecret = "wvQsxgvlaFvvJu7sGw2rzIIiWXC5GwSIwMpSq7VWRRwkYx0kbs05OGhsTR2C3Wc7",
                    passKey = "MTc0Mzc5YmZiMjc5ZjlhYTliZGJjZjE1OGU5N2RkNzFhNDY3Y2QyZTBjODkzMDU5YjEwZjc4ZTZiNzJhZGExZWQyYzkxOTIwMjUwNDA2MDkyOTQ3",
                    businessShortCode = "174379",
                    callbackUrl = "https://mydomain.com/path"
                )
            ),
            MpesaTransactionRepository()
        )
    )
) {
    val transactions by viewModel.transactions.collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transaction History") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (transactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("No transactions found")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(transactions) { transaction ->
                    TransactionHistoryItem(transaction)
                }
            }
        }
    }
}

@Composable
fun TransactionHistoryItem(transaction: MpesaTransaction) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status Icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .padding(end = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (transaction.status) {
                        "COMPLETED" -> Icons.Default.CheckCircle
                        "PENDING" -> Icons.Default.AddCircle
                        else -> Icons.Default.Warning
                    },
                    contentDescription = "Transaction Status",
                    tint = when (transaction.status) {
                        "COMPLETED" -> MaterialTheme.colorScheme.primary
                        "PENDING" -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.error
                    }
                )
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "KES ${transaction.amount}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = formatTransactionDate(transaction),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = transaction.phoneNumber,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Status chip
                Surface(
                    modifier = Modifier.padding(top = 4.dp),
                    shape = CircleShape,
                    color = when (transaction.status) {
                        "COMPLETED" -> MaterialTheme.colorScheme.primaryContainer
                        "PENDING" -> MaterialTheme.colorScheme.tertiaryContainer
                        else -> MaterialTheme.colorScheme.errorContainer
                    }
                ) {
                    Text(
                        text = transaction.status,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = when (transaction.status) {
                            "COMPLETED" -> MaterialTheme.colorScheme.onPrimaryContainer
                            "PENDING" -> MaterialTheme.colorScheme.onTertiaryContainer
                            else -> MaterialTheme.colorScheme.onErrorContainer
                        }
                    )
                }
                
                // Show result description if available
                transaction.resultDesc?.let { desc ->
                    Text(
                        text = desc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun formatTransactionDate(transaction: MpesaTransaction): String {
    return try {
        SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
            .format(Date(transaction.timestamp))
    } catch (e: Exception) {
        "Unknown date"
    }
}

private fun createDarajaClient(): DarajaClient {
    val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    
    val httpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
    
    val retrofit = Retrofit.Builder()
        .baseUrl("https://sandbox.safaricom.co.ke/")
        .client(httpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    return retrofit.create(DarajaClient::class.java)
} 