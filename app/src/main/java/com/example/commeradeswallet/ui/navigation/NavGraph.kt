package com.example.commeradeswallet.ui.navigation

import androidx.compose.runtime.*
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.commeradeswallet.ui.screens.auth.AuthScreen
import com.example.commeradeswallet.ui.screens.auth.RegisterScreen
import com.example.commeradeswallet.ui.screens.dashboard.DashboardScreen
import com.example.commeradeswallet.ui.screens.cart.OrderSummaryScreen
import com.example.commeradeswallet.ui.screens.wallet.WalletScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.commeradeswallet.ui.viewmodel.CartViewModel
import com.example.commeradeswallet.ui.screens.topup.TopUpScreen
import android.util.Log
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.example.commeradeswallet.auth.GoogleAuthClient
import kotlinx.coroutines.launch

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String = "auth"
) {
    val context = LocalContext.current
    val googleAuthClient = remember { GoogleAuthClient(context) }
    val scope = rememberCoroutineScope()
    
    // Check if user is already signed in
    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val user = googleAuthClient.getSignedInUser()
                if (user != null) {
                    Log.d("NavGraph", "User already signed in, navigating to dashboard")
                    navController.navigate("dashboard") {
                        popUpTo("auth") { inclusive = true }
                    }
                }
            } catch (e: Exception) {
                Log.e("NavGraph", "Error checking auth state", e)
            }
        }
    }

    // Create shared CartViewModel
    val cartViewModel: CartViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable("auth") {
            AuthScreen(
                onNavigateToRegister = { 
                    Log.d("NavGraph", "Navigating to register")
                    navController.navigate("register") 
                },
                onNavigateToHome = { 
                    Log.d("NavGraph", "Navigating to dashboard from auth")
                    navController.navigate("dashboard") {
                        popUpTo("auth") { inclusive = true }
                    }
                }
            )
        }
        composable("register") {
            RegisterScreen(
                onNavigateToLogin = { 
                    Log.d("NavGraph", "Navigating back to login")
                    navController.navigateUp() 
                },
                onNavigateToHome = { 
                    Log.d("NavGraph", "Navigating to dashboard from register")
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
                cartViewModel = cartViewModel,
                onSignOut = {
                    scope.launch {
                        try {
                            googleAuthClient.signOut()
                            navController.navigate("auth") {
                                popUpTo(0) { inclusive = true }
                            }
                        } catch (e: Exception) {
                            Log.e("NavGraph", "Error signing out", e)
                        }
                    }
                }
            )
        }
        composable("cart") {
            OrderSummaryScreen(
                onNavigateBack = { navController.navigateUp() },
                onCheckout = { /* TODO: Implement checkout */ },
                cartViewModel = cartViewModel
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
                onNavigateBack = { navController.navigateUp() }
            )
        }
    }
} 