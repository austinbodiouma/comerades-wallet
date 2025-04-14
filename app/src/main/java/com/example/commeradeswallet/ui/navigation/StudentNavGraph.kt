package com.example.commeradeswallet.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.commeradeswallet.ui.screens.auth.AuthScreen
import com.example.commeradeswallet.ui.screens.auth.RegisterScreen
import com.example.commeradeswallet.ui.screens.cart.OrderSummaryScreen
import com.example.commeradeswallet.ui.screens.dashboard.DashboardScreen
import com.example.commeradeswallet.ui.screens.wallet.TopUpScreen
import com.example.commeradeswallet.ui.screens.wallet.TransactionHistoryScreen
import com.example.commeradeswallet.ui.screens.wallet.WalletScreen
import com.example.commeradeswallet.ui.viewmodel.CartViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun StudentNavGraph(
    navController: NavHostController,
    startDestination: String = "auth"
) {
    // Create a shared CartViewModel instance
    val cartViewModel: CartViewModel = viewModel()
    
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable("auth") {
            AuthScreen(
                onNavigateToRegister = { navController.navigate("register") },
                onNavigateToHome = {
                    navController.navigate("dashboard") {
                        popUpTo("auth") { inclusive = true }
                    }
                }
            )
        }

        composable("register") {
            RegisterScreen(
                onNavigateToLogin = { navController.navigateUp() },
                onNavigateToHome = {
                    navController.navigate("dashboard") {
                        popUpTo("register") { inclusive = true }
                    }
                }
            )
        }

        composable("dashboard") {
            DashboardScreen(
                onNavigateToCart = { navController.navigate("cart") },
                onNavigateToWallet = { navController.navigate("wallet") },
                cartViewModel = cartViewModel, // Pass the shared ViewModel
                onSignOut = {
                    navController.navigate("auth") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable("cart") {
            OrderSummaryScreen(
                onNavigateBack = { navController.navigateUp() },
                cartViewModel = cartViewModel, // Pass the shared ViewModel
                onCheckout = { 
                    // Navigate to payment screen or process payment
                    navController.navigate("wallet")
                }
            )
        }

        composable("wallet") {
            WalletScreen(
                onNavigateToTopUp = { navController.navigate("topup") },
                onNavigateBack = { navController.navigateUp() }
            )
        }
        
        composable("topup") {
            TopUpScreen(
                onNavigateBack = { navController.navigateUp() },
                onNavigateToHistory = { navController.navigate("transaction_history") }
            )
        }
        
        composable("transaction_history") {
            TransactionHistoryScreen(
                onNavigateBack = { navController.navigateUp() }
            )
        }
    }
} 