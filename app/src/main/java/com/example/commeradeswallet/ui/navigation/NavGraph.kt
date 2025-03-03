import androidx.compose.runtime.Composable
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

@Composable
fun NavGraph(
    navController: NavHostController
) {
    // Create shared CartViewModel
    val cartViewModel: CartViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = "auth"
    ) {
        composable("auth") {
            AuthScreen(
                onNavigateToRegister = { navController.navigate("register") },
                onNavigateToHome = { 
                    Log.d("Navigation", "Attempting to navigate to dashboard")
                    navController.navigate("dashboard") {
                        popUpTo("auth") { 
                            inclusive = true  // This removes the auth screen from back stack
                        }
                        launchSingleTop = true  // Avoid multiple copies of dashboard
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
                cartViewModel = cartViewModel
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