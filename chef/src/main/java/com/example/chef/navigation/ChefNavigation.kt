package com.example.chef.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.chef.screens.inventory.InventoryScreen

@Composable
fun ChefNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "inventory") {
        composable("inventory") {
            InventoryScreen()
        }
    }
} 