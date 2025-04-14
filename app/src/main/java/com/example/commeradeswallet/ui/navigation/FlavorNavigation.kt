package com.example.commeradeswallet.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation.compose.rememberNavController
import com.example.commeradeswallet.BuildConfig

/**
 * Entry point for flavor-specific navigation.
 * This composable handles the setup of the proper navigation graph
 * based on the build flavor.
 */
@Composable
fun FlavorNavigation() {
    // Get the current view model store owner to provide to navigation controller
    val viewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current) {
        "No ViewModelStoreOwner was provided via LocalViewModelStoreOwner"
    }
    
    // Create a NavController with the current context
    val navController = rememberNavController().apply {
        // Set the ViewModelStore before setting the graph
        setViewModelStore(viewModelStoreOwner.viewModelStore)
    }

    // Default to student flavor if FLAVOR is not defined
    val flavor = try {
        BuildConfig::class.java.getField("FLAVOR").get(null) as String
    } catch (e: Exception) {
        "student"
    }
    
    when (flavor) {
        "student" -> StudentNavGraph(
            navController = navController
        )
        "cashier" -> CashierNavGraph(
            navController = navController
        )
        else -> StudentNavGraph( // Default to student navigation
            navController = navController
        )
    }
} 