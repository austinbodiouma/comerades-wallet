package com.example.commeradeswallet.ui.screens.cart

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.commeradeswallet.BuildConfig
import com.example.commeradeswallet.data.model.CartItem
import com.example.commeradeswallet.data.model.FoodItem
import com.example.commeradeswallet.data.mpesa.DarajaClient
import com.example.commeradeswallet.data.mpesa.DarajaRepository
import com.example.commeradeswallet.data.repository.MpesaRepository
import com.example.commeradeswallet.data.repository.MpesaTransactionRepository
import com.example.commeradeswallet.ui.viewmodel.CartViewModel
import com.example.commeradeswallet.ui.viewmodel.MpesaViewModel
import com.example.commeradeswallet.ui.preview.PreviewWrapper
import com.example.commeradeswallet.ui.preview.ThemePreviews
import com.example.commeradeswallet.ui.screens.wallet.format
import com.example.commeradeswallet.ui.screens.wallet.createMpesaViewModel
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderSummaryScreen(
    onNavigateBack: () -> Unit,
    onCheckout: () -> Unit,
    cartViewModel: CartViewModel = viewModel(),
    mpesaViewModel: MpesaViewModel = createMpesaViewModel()
) {
    val cartItems by cartViewModel.cartItems.collectAsState()
    val totalAmount by cartViewModel.totalAmount.collectAsState()
    var showMpesaDialog by remember { mutableStateOf(false) }
    
    val mpesaState by mpesaViewModel.state.collectAsState()
    val transactionState by mpesaViewModel.transactionState.collectAsState()

    // Reset M-Pesa state when screen is shown
    LaunchedEffect(Unit) {
        mpesaViewModel.resetState()
    }

    // Process M-Pesa state changes
    LaunchedEffect(transactionState) {
        if (transactionState is MpesaViewModel.TransactionState.Success) {
            // Payment was successful, wait for a moment then navigate
            delay(3000)
            onCheckout()
        }
    }

    // M-Pesa Payment Dialog
    if (showMpesaDialog) {
        MpesaPaymentDialog(
            onDismiss = { 
                showMpesaDialog = false 
                if (transactionState !is MpesaViewModel.TransactionState.Success) {
                    mpesaViewModel.resetState()
                }
            },
            onPaymentSubmit = { phoneNumber ->
                mpesaViewModel.initiateStk(phoneNumber, totalAmount.toInt())
            },
            mpesaState = mpesaState,
            transactionState = transactionState,
            amount = totalAmount.toInt()
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cart") },
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
        ) {
            if (cartItems.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Your cart is empty",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(cartItems) { item ->
                        CartItemCard(
                            cartItem = item,
                            onUpdateQuantity = { quantity ->
                                cartViewModel.updateQuantity(item.foodItem, quantity)
                            }
                        )
                    }
                }

                // Total and Checkout
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    tonalElevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Total",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                "Ksh ${totalAmount.format(2)}",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        
                        // Pay with M-Pesa Button
                        Button(
                            onClick = { showMpesaDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                            enabled = cartItems.isNotEmpty()
                        ) {
                            Text("Pay with M-Pesa")
                        }
                        
                        // Use wallet button
                        OutlinedButton(
                            onClick = onCheckout,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            enabled = cartItems.isNotEmpty()
                        ) {
                            Text("Pay with Wallet")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MpesaPaymentDialog(
    onDismiss: () -> Unit,
    onPaymentSubmit: (String) -> Unit,
    mpesaState: MpesaViewModel.MpesaState,
    transactionState: MpesaViewModel.TransactionState,
    amount: Int
) {
    var phoneNumber by remember { mutableStateOf("") }
    var phoneError by remember { mutableStateOf<String?>(null) }
    
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
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "M-Pesa Payment",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    "Amount: KSh $amount",
                    style = MaterialTheme.typography.titleMedium
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                when (mpesaState) {
                    is MpesaViewModel.MpesaState.Idle -> {
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
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            "Enter your M-Pesa phone number",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = onDismiss) {
                                Text("Cancel")
                            }
                            
                            Button(
                                onClick = {
                                    val formattedPhone = formatPhoneNumber(phoneNumber)
                                    if (!validatePhoneNumber(phoneNumber)) {
                                        phoneError = "Please enter a valid Kenyan phone number"
                                        return@Button
                                    }
                                    
                                    onPaymentSubmit(formattedPhone)
                                },
                                enabled = phoneNumber.isNotEmpty()
                            ) {
                                Text("Pay Now")
                            }
                        }
                    }
                    
                    is MpesaViewModel.MpesaState.Loading -> {
                        CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                        Text(
                            "Initiating payment...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    
                    is MpesaViewModel.MpesaState.Success -> {
                        // Show STK push sent message
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(64.dp)
                                .padding(8.dp)
                        )
                        
                        Text(
                            "STK Push Sent",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Text(
                            "Please check your phone and enter your M-Pesa PIN to complete payment.",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                        
                        when (transactionState) {
                            is MpesaViewModel.TransactionState.Success -> {
                                Icon(
                                    imageVector = Icons.Default.ShoppingCart,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .size(64.dp)
                                        .padding(8.dp)
                                )
                                
                                Text(
                                    "Payment Successful!",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                
                                LinearProgressIndicator(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp)
                                )
                                
                                Text("Processing your order...")
                            }
                            
                            is MpesaViewModel.TransactionState.Failed -> {
                                Text(
                                    (transactionState as MpesaViewModel.TransactionState.Failed).message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error
                                )
                                
                                Button(
                                    onClick = onDismiss,
                                    modifier = Modifier.padding(top = 16.dp)
                                ) {
                                    Text("Close")
                                }
                            }
                            
                            else -> {
                                // Show progress indicator for transaction state
                                LinearProgressIndicator(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp)
                                )
                                
                                Text(
                                    "Verifying payment...",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                
                                TextButton(
                                    onClick = onDismiss,
                                    modifier = Modifier.padding(top = 8.dp)
                                ) {
                                    Text("Cancel")
                                }
                            }
                        }
                    }
                    
                    is MpesaViewModel.MpesaState.Error -> {
                        Text(
                            (mpesaState as MpesaViewModel.MpesaState.Error).message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.padding(top = 16.dp)
                        ) {
                            Text("Try Again")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CartItemCard(
    cartItem: CartItem,
    onUpdateQuantity: (Int) -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = cartItem.foodItem.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Ksh ${cartItem.foodItem.price} × ${cartItem.quantity}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Text(
                    text = "Ksh ${cartItem.foodItem.price * cartItem.quantity}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            QuantityControls(
                quantity = cartItem.quantity,
                onQuantityChange = onUpdateQuantity
            )
        }
    }
}

@Composable
private fun QuantityControls(
    quantity: Int,
    onQuantityChange: (Int) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            modifier = Modifier.size(32.dp),
            shape = CircleShape,
            color = if (quantity > 0) 
                MaterialTheme.colorScheme.primary 
            else 
                MaterialTheme.colorScheme.surfaceVariant,
            onClick = { if (quantity > 0) onQuantityChange(quantity - 1) }
        ) {
            Icon(
                Icons.Default.Clear,
                contentDescription = "Decrease",
                tint = if (quantity > 0) 
                    MaterialTheme.colorScheme.onPrimary 
                else 
                    MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(4.dp)
            )
        }

        Text(
            text = quantity.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Surface(
            modifier = Modifier.size(32.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            onClick = { onQuantityChange(quantity + 1) }
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "Increase",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(4.dp)
            )
        }
    }
}

@ThemePreviews
@Composable
fun OrderSummaryScreenPreview() {
    val previewCartViewModel = CartViewModel().apply {
        updateQuantity(
            FoodItem(
                id = "1",
                name = "Chapati",
                price = 20,
                category = "Breakfast",
                imageUrl = "",
                description = "Fresh chapati",
                isQuantifiedByNumber = true
            ),
            2
        )
        updateQuantity(
            FoodItem(
                id = "2",
                name = "Rice",
                price = 50,
                category = "Lunch",
                imageUrl = "",
                description = "Steamed rice",
                isQuantifiedByNumber = false
            ),
            1
        )
    }

    PreviewWrapper {
        OrderSummaryScreen(
            onNavigateBack = {},
            onCheckout = {},
            cartViewModel = previewCartViewModel
        )
    }
} 