package com.example.commeradeswallet.ui.navigation

import androidx.compose.runtime.*
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.compose.ui.platform.LocalContext
import android.util.Log
import com.example.commeradeswallet.auth.GoogleAuthClient
import com.example.commeradeswallet.ui.screens.auth.AuthScreen
import com.example.cashier.ui.screens.CashierScreen
import com.example.cashier.ui.screens.OrdersScreen
import com.example.cashier.ui.screens.SalesScreen
import com.example.cashier.ui.screens.StockScreen
import kotlinx.coroutines.launch
import androidx.navigation.NavGraphBuilder
import androidx.navigation.navigation

@Composable
fun CashierNavGraph(
    navController: NavHostController,
    startDestination: String = "cashier"
) {
    val context = LocalContext.current
    val googleAuthClient = remember { GoogleAuthClient(context) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val user = googleAuthClient.getSignedInUser()
                if (user != null) {
                    navController.navigate("cashier_dashboard") {
                        popUpTo("auth") { inclusive = true }
                    }
                }
            } catch (e: Exception) {
                Log.e("CashierNavGraph", "Error checking auth state", e)
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable("auth") {
            AuthScreen(
                onNavigateToRegister = { },
                onNavigateToHome = {
                    navController.navigate("cashier_dashboard") {
                        popUpTo("auth") { inclusive = true }
                    }
                }
            )
        }
        
        composable("cashier_dashboard") {
            CashierScreen(
                onNavigateBack = {
                    scope.launch {
                        try {
                            googleAuthClient.signOut()
                            navController.navigate("auth") {
                                popUpTo(0) { inclusive = true }
                            }
                        } catch (e: Exception) {
                            Log.e("CashierNavGraph", "Error signing out", e)
                        }
                    }
                }
            )
        }
        
        cashierNavGraph(navController)
    }
}

fun NavGraphBuilder.cashierNavGraph(
    navController: NavHostController
) {
    navigation(startDestination = "cashier", route = "cashier_graph") {
        composable("cashier") {
            CashierScreen(
                onNavigateToOrders = { navController.navigate("orders") },
                onNavigateToSales = { navController.navigate("sales") },
                onNavigateToStock = { navController.navigate("stock") }
            )
        }
        
        composable("orders") {
            OrdersScreen()
        }
        
        composable("sales") {
            SalesScreen(
                onNavigateBack = { navController.navigateUp() }
            )
        }
        
        composable("stock") {
            StockScreen()
        }
    }
} 