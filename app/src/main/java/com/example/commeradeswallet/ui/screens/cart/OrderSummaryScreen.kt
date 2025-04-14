package com.example.commeradeswallet.ui.screens.cart

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.commeradeswallet.data.model.CartItem
import com.example.commeradeswallet.data.model.FoodItem
import com.example.commeradeswallet.ui.viewmodel.CartViewModel
import com.example.commeradeswallet.ui.preview.PreviewWrapper
import com.example.commeradeswallet.ui.preview.ThemePreviews
import com.example.commeradeswallet.ui.screens.wallet.format

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderSummaryScreen(
    onNavigateBack: () -> Unit,
    onCheckout: () -> Unit,
    cartViewModel: CartViewModel = viewModel()
) {
    val cartItems by cartViewModel.cartItems.collectAsState()
    val totalAmount by cartViewModel.totalAmount.collectAsState()

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
                        Button(
                            onClick = onCheckout,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                            enabled = cartItems.isNotEmpty()
                        ) {
                            Text("Proceed to Checkout")
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