package com.example.commeradeswallet.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.example.commeradeswallet.data.model.CartItem
import com.example.commeradeswallet.data.model.FoodItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class CartViewModel : ViewModel() {
    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems

    private val _totalAmount = MutableStateFlow(0.0)
    val totalAmount: StateFlow<Double> = _totalAmount

    fun updateQuantity(foodItem: FoodItem, quantity: Int) {
        _cartItems.update { currentItems ->
            val existingItem = currentItems.find { it.foodItem.id == foodItem.id }
            if (quantity <= 0) {
                // Remove item if quantity is 0 or negative
                currentItems.filter { it.foodItem.id != foodItem.id }
            } else if (existingItem != null) {
                // Update existing item quantity
                currentItems.map { item ->
                    if (item.foodItem.id == foodItem.id) {
                        item.copy(quantity = quantity)
                    } else {
                        item
                    }
                }
            } else {
                // Add new item
                currentItems + CartItem(foodItem, quantity)
            }
        }
        updateTotalAmount()
    }

    private fun updateTotalAmount() {
        _totalAmount.value = _cartItems.value.sumOf { 
            it.foodItem.price * it.quantity 
        }.toDouble()
    }

    fun clearCart() {
        _cartItems.value = emptyList()
        _totalAmount.value = 0.0
    }
} 
