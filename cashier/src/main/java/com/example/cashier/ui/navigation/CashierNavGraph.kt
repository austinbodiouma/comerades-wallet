package com.example.cashier.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.cashier.ui.screens.CashierScreen

@Composable
fun CashierNavGraph(
    navController: NavHostController,
    startDestination: String = "cashier_home"
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable("cashier_home") {
            CashierScreen(
                onNavigateBack = {
                    // Handle sign out or navigation
                }
            )
        }
    }
} 