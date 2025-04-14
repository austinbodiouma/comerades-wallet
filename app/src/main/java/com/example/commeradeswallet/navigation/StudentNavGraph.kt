package com.example.commeradeswallet.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.commeradeswallet.ui.screens.auth.AuthScreen
import com.example.commeradeswallet.ui.screens.auth.RegisterScreen
import com.example.commeradeswallet.ui.screens.dashboard.DashboardScreen
import com.example.commeradeswallet.ui.screens.wallet.TopUpScreen
import com.example.commeradeswallet.ui.screens.wallet.TransactionHistoryScreen

@Composable
fun StudentNavGraph(
    navController: NavHostController,
    startDestination: String = "auth"
) {
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
                        popUpTo("auth") { inclusive = true }
                    }
                }
            )
        }

        composable("dashboard") {
            DashboardScreen(
                onNavigateToCart = { navController.navigate("cart") },
                onNavigateToWallet = { navController.navigate("wallet") },
                onSignOut = {
                    navController.navigate("auth") {
                        popUpTo("dashboard") { inclusive = true }
                    }
                }
            )
        }

        composable("top_up") {
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