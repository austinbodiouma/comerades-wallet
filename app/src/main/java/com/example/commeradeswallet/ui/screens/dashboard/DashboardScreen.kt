package com.example.commeradeswallet.ui.screens.dashboard

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.commeradeswallet.R
import com.example.commeradeswallet.data.model.FoodItem
import com.example.commeradeswallet.ui.preview.PreviewWrapper
import com.example.commeradeswallet.ui.preview.ThemePreviews
import com.example.commeradeswallet.ui.viewmodel.AuthViewModel
import com.example.commeradeswallet.ui.viewmodel.AuthViewModelFactory
import com.example.commeradeswallet.ui.viewmodel.CartViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    onNavigateToCart: () -> Unit,
    onNavigateToWallet: () -> Unit,
    viewModel: DashboardViewModel = viewModel(factory = DashboardViewModel.Factory),
    cartViewModel: CartViewModel = viewModel(),
    onSignOut: () -> Unit,
    authViewModel: AuthViewModel = viewModel(factory = AuthViewModelFactory(LocalContext.current))
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    
    val foodItems by viewModel.foodItems.collectAsState()
    val categories = listOf("All", "Main Course", "Vegetables", "Stew")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Food Menu", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onNavigateToWallet) {
                        Icon(Icons.Default.MailOutline, contentDescription = "Wallet")
                    }
                    IconButton(onClick = onNavigateToCart) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = "Cart")
                    }
                    IconButton(
                        onClick = {
                            authViewModel.signOut()
                            onSignOut()
                        }
                    ) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Sign Out")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search Bar
            SearchBar(
                query = searchQuery,
                onQueryChange = { 
                    searchQuery = it
                    viewModel.searchFoodItems(it)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Search food items...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = if (searchQuery.isNotEmpty()) {
                    {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                } else null,
                active = false,
                onSearch = {},
                onActiveChange = {}
            ) {}

            // Categories
            ScrollableTabRow(
                selectedTabIndex = categories.indexOf(selectedCategory ?: "All"),
                modifier = Modifier.padding(vertical = 8.dp),
                edgePadding = 16.dp,
                divider = {},
                indicator = {},
                containerColor = Color.Transparent
            ) {
                categories.forEach { category ->
                    Tab(
                        selected = category == selectedCategory,
                        onClick = { 
                            selectedCategory = category
                            viewModel.filterByCategory(category)
                        },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (category == selectedCategory) 
                                MaterialTheme.colorScheme.primaryContainer 
                            else 
                                MaterialTheme.colorScheme.surface,
                            modifier = Modifier.padding(vertical = 8.dp),
                            tonalElevation = 2.dp
                        ) {
                            Text(
                                text = category,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                color = if (category == selectedCategory)
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                else
                                    MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Food Items Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(foodItems) { foodItem ->
                    FoodItemCard(
                        foodItem = foodItem,
                        cartViewModel = cartViewModel
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FoodItemCard(
    foodItem: FoodItem,
    cartViewModel: CartViewModel
) {
    var quantity by remember { mutableStateOf(0) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        onClick = {},
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Food Image
            Image(
                painter = painterResource(getFoodImage(foodItem.name)),
                contentDescription = foodItem.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = foodItem.name,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                )
            )

            Text(
                text = "Ksh ${foodItem.price}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = if (foodItem.isQuantifiedByNumber) "per piece" else "per serving",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Quantity controls
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { 
                                if (quantity > 0) {
                                    quantity--
                                    cartViewModel.updateQuantity(foodItem, quantity)
                                }
                            },
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(if (quantity > 0) 
                                    MaterialTheme.colorScheme.primary 
                                else 
                                    MaterialTheme.colorScheme.surfaceVariant
                                )
                        ) {
                            Icon(
                                Icons.Default.Clear,
                                contentDescription = "Decrease",
                                tint = if (quantity > 0) 
                                    MaterialTheme.colorScheme.onPrimary 
                                else 
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text(
                            text = quantity.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        IconButton(
                            onClick = { 
                                quantity++
                                cartViewModel.updateQuantity(foodItem, quantity)
                            },
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(
                                Icons.Default.Add, 
                                contentDescription = "Increase",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun getFoodImage(foodName: String): Int {
    return when (foodName.lowercase()) {
        "chapati" -> R.drawable.chapati
        "rice" -> R.drawable.rice
        "beans" -> R.drawable.beans
        "githeri" -> R.drawable.githeri
        "kales" -> R.drawable.kales
        "cabbage" -> R.drawable.cabbage
        "beef stew" -> R.drawable.beef_stew
        else -> R.drawable.chapati
    }
}

@ThemePreviews
@Composable
private fun DashboardScreenPreview() {
    PreviewWrapper {
        DashboardScreen(
            onNavigateToCart = {},
            onNavigateToWallet = {},
            cartViewModel = CartViewModel(),
            onSignOut = {},
            viewModel = DashboardViewModel.previewViewModel()
        )
    }
}