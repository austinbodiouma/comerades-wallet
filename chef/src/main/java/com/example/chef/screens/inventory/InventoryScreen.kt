package com.example.chef.screens.inventory

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.chef.data.model.MenuItem
import com.example.chef.ui.viewmodel.InventoryViewModel
import com.example.chef.ui.viewmodel.InventoryViewModel.MenuItemsState
import androidx.compose.ui.platform.LocalContext

private const val TAG = "InventoryScreen"

data class FoodItem(
    val id: String = "",
    val name: String = "",
    val quantity: Int = 0,
    val isAvailable: Boolean = true,
    val price: Double = 0.0
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(viewModel: InventoryViewModel = viewModel(factory = InventoryViewModel.Factory())) {
    val TAG = "InventoryScreen"
    
    Scaffold { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (val currentState = viewModel.state.collectAsState().value) {
                is MenuItemsState.Loading -> {
                    LoadingScreen()
                }
                is MenuItemsState.Success -> {
                    InventoryContent(
                        items = currentState.items,
                        onToggleAvailability = viewModel::toggleItemAvailability
                    )
                }
                is MenuItemsState.Error -> {
                    ErrorScreen(
                        message = currentState.message,
                        onRetry = { viewModel.loadMenuItems() }
                    )
                }
                is MenuItemsState.Empty -> {
                    EmptyInventoryScreen()
                }
            }
        }
    }
}

@Composable
fun EmptyInventoryScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "No menu items found",
            style = MaterialTheme.typography.titleLarge
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryContent(
    items: List<MenuItem>,
    onToggleAvailability: (MenuItem) -> Unit
) {
    val groupedItems = remember(items) { items.groupBy { it.category } }
    val categories = remember(groupedItems) { groupedItems.keys.toList() }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        categories.forEach { category ->
            item(key = "header_$category") {
                Text(
                    text = category,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            val categoryItems = groupedItems[category] ?: emptyList()
            items(
                items = categoryItems,
                key = { it.id }
            ) { item ->
                MenuItemCard(
                    item = item,
                    onToggleAvailability = {
                        Log.d(TAG, "Toggling availability for item: ${it.name}")
                        onToggleAvailability(it)
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuItemCard(
    item: MenuItem,
    onToggleAvailability: (MenuItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = { 
            Log.d(TAG, "Card clicked for item: ${item.name}")
            onToggleAvailability(item)
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "KES ${item.price}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (item.isAvailable) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = if (item.isAvailable) "Available" else "Not Available",
                            tint = if (item.isAvailable) Color.Green else Color.Red
                        )
                        Text(
                            text = if (item.isAvailable) "Available" else "Not Available",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (item.isAvailable) Color.Green else Color.Red
                        )
                    }
                    if (item.isQuantifiedByNumber) {
                        Text(
                            text = "Sold by quantity",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodItemCard(
    food: FoodItem,
    onEdit: (FoodItem) -> Unit,
    onUpdateQuantity: (FoodItem, Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = food.name,
                    style = MaterialTheme.typography.titleMedium
                )
                IconButton(onClick = { onEdit(food) }) {
                    Icon(Icons.Default.Edit, "Edit")
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text("Price: Ksh ${food.price}")
            Text(
                "Status: ${if (food.isAvailable) "Available" else "Not Available"}",
                color = if (food.isAvailable) MaterialTheme.colorScheme.primary 
                       else MaterialTheme.colorScheme.error
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Quantity: ${food.quantity}")
                Row {
                    Button(
                        onClick = { onUpdateQuantity(food, food.quantity - 1) },
                        enabled = food.quantity > 0
                    ) {
                        Text("-")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onUpdateQuantity(food, food.quantity + 1) }
                    ) {
                        Text("+")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditFoodDialog(
    food: FoodItem?,
    onDismiss: () -> Unit,
    onSave: (name: String, price: Double) -> Unit
) {
    var name by remember { mutableStateOf(food?.name ?: "") }
    var priceText by remember { mutableStateOf(food?.price?.toString() ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (food == null) "Add Food Item" else "Edit Food Item") },
        text = {
            Column {
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Food Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    label = { Text("Price (Ksh)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val price = priceText.toDoubleOrNull() ?: 0.0
                    if (name.isNotBlank() && price > 0) {
                        onSave(name, price)
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
fun ErrorScreen(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Error: $message",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error
            )
            Button(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
} 