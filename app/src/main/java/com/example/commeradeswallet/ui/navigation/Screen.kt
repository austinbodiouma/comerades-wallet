package com.example.commeradeswallet.ui.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object Home : Screen("home")
    object Dashboard : Screen("dashboard")
    object Cart : Screen("cart")
    object Wallet : Screen("wallet")
    object TopUp : Screen("topup")
    object Cashier : Screen("cashier")
    object TransactionHistory : Screen("transaction_history")
} 