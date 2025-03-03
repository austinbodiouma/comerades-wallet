package com.example.commeradeswallet.ui.screens.wallet

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.commeradeswallet.data.model.TransactionType
import com.example.commeradeswallet.data.model.WalletTransaction
import com.example.commeradeswallet.util.format
import java.time.format.DateTimeFormatter
import com.example.commeradeswallet.ui.preview.PreviewWrapper
import com.example.commeradeswallet.ui.preview.ThemePreviews
import com.example.commeradeswallet.ui.preview.PreviewData

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun TransactionCard(transaction: WalletTransaction) {
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
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = when (transaction.type) {
                        TransactionType.DEPOSIT -> MaterialTheme.colorScheme.primaryContainer
                        else -> MaterialTheme.colorScheme.errorContainer
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = if (transaction.type == TransactionType.DEPOSIT) 
                            Icons.Default.Add else Icons.Default.Clear,
                        contentDescription = null,
                        tint = when (transaction.type) {
                            TransactionType.DEPOSIT -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.error
                        },
                        modifier = Modifier.padding(8.dp)
                    )
                }

                Column {
                    Text(
                        text = transaction.description,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = transaction.timestamp.format(
                            DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            Text(
                text = "${if (transaction.type == TransactionType.DEPOSIT) "+" else "-"}Ksh ${
                    transaction.amount.format(2)
                }",
                style = MaterialTheme.typography.titleMedium,
                color = when (transaction.type) {
                    TransactionType.DEPOSIT -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.error
                },
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@ThemePreviews
@Composable
private fun TransactionCardPreview() {
    PreviewWrapper {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            // Preview deposit transaction
            TransactionCard(PreviewData.sampleTransactions[0])
            
            // Preview withdrawal transaction
            TransactionCard(PreviewData.sampleTransactions[1])
        }
    }
} 