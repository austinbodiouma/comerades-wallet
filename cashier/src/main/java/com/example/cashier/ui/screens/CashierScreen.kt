package com.example.cashier.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cashier.ui.viewmodel.CashierViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashierScreen(
    modifier: Modifier = Modifier,
    viewModel: CashierViewModel = viewModel(),
    onNavigateBack: () -> Unit = {},
    onNavigateToOrders: () -> Unit = {},
    onNavigateToSales: () -> Unit = {},
    onNavigateToStock: () -> Unit = {}
) {
    var orderCode by remember { mutableStateOf("") }
    val orderState by viewModel.orderState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cashier Dashboard") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Navigation buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(onClick = onNavigateToOrders) {
                    Text("Orders")
                }
                Button(onClick = onNavigateToSales) {
                    Text("Sales")
                }
                Button(onClick = onNavigateToStock) {
                    Text("Stock")
                }
            }

            OutlinedTextField(
                value = orderCode,
                onValueChange = { orderCode = it },
                label = { Text("Enter Order Code") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = { viewModel.verifyOrder(orderCode) },
                modifier = Modifier.fillMaxWidth(),
                enabled = orderCode.isNotBlank()
            ) {
                Text("Verify Order")
            }

            when (val state = orderState) {
                is CashierViewModel.OrderState.Success -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "Order Details",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text("Order ID: ${state.order.id}")
                            Text("Total Amount: KES ${state.order.totalAmount}")
                            Text("Status: ${state.order.status}")
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Text(
                                "Items:",
                                style = MaterialTheme.typography.titleMedium
                            )
                            state.order.items.forEach { item ->
                                Text("${item.name} x${item.quantity} @ KES ${item.price}")
                            }

                            if (state.order.status == "PENDING") {
                                Button(
                                    onClick = { viewModel.approveOrder(state.order.id) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Approve Order")
                                }
                            }
                        }
                    }
                }
                is CashierViewModel.OrderState.Error -> {
                    Text(
                        state.message,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                CashierViewModel.OrderState.Loading -> {
                    CircularProgressIndicator()
                }
                CashierViewModel.OrderState.Initial -> {
                    // Show nothing initially
                }
            }
        }
    }
} 