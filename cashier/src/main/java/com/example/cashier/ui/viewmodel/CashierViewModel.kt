package com.example.cashier.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class CashierViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()
    
    private val _orderState = MutableStateFlow<OrderState>(OrderState.Initial)
    val orderState: StateFlow<OrderState> = _orderState

    fun verifyOrder(orderCode: String) {
        viewModelScope.launch {
            try {
                _orderState.value = OrderState.Loading

                val orderSnapshot = firestore.collection("orders")
                    .whereEqualTo("orderCode", orderCode)
                    .get()
                    .await()

                if (orderSnapshot.isEmpty) {
                    _orderState.value = OrderState.Error("Order not found")
                    return@launch
                }

                val orderDoc = orderSnapshot.documents.first()
                val order = Order(
                    id = orderDoc.id,
                    orderCode = orderDoc.getString("orderCode") ?: "",
                    totalAmount = orderDoc.getDouble("totalAmount") ?: 0.0,
                    status = orderDoc.getString("status") ?: "UNKNOWN",
                    items = (orderDoc.get("items") as? List<Map<String, Any>>)?.map { item ->
                        OrderItem(
                            name = item["name"] as? String ?: "",
                            quantity = (item["quantity"] as? Long)?.toInt() ?: 0,
                            price = (item["price"] as? Long)?.toInt() ?: 0
                        )
                    } ?: emptyList()
                )

                _orderState.value = OrderState.Success(order)
            } catch (e: Exception) {
                Log.e("CashierViewModel", "Error verifying order", e)
                _orderState.value = OrderState.Error(e.message ?: "Failed to verify order")
            }
        }
    }

    fun approveOrder(orderId: String) {
        viewModelScope.launch {
            try {
                _orderState.value = OrderState.Loading

                firestore.collection("orders")
                    .document(orderId)
                    .update(
                        mapOf(
                            "status" to "APPROVED",
                            "approvedAt" to com.google.firebase.Timestamp.now()
                        )
                    )
                    .await()

                // Refresh order details
                val updatedOrder = firestore.collection("orders")
                    .document(orderId)
                    .get()
                    .await()

                val order = Order(
                    id = updatedOrder.id,
                    orderCode = updatedOrder.getString("orderCode") ?: "",
                    totalAmount = updatedOrder.getDouble("totalAmount") ?: 0.0,
                    status = updatedOrder.getString("status") ?: "UNKNOWN",
                    items = (updatedOrder.get("items") as? List<Map<String, Any>>)?.map { item ->
                        OrderItem(
                            name = item["name"] as? String ?: "",
                            quantity = (item["quantity"] as? Long)?.toInt() ?: 0,
                            price = (item["price"] as? Long)?.toInt() ?: 0
                        )
                    } ?: emptyList()
                )

                _orderState.value = OrderState.Success(order)
            } catch (e: Exception) {
                Log.e("CashierViewModel", "Error approving order", e)
                _orderState.value = OrderState.Error(e.message ?: "Failed to approve order")
            }
        }
    }

    sealed class OrderState {
        object Initial : OrderState()
        object Loading : OrderState()
        data class Success(val order: Order) : OrderState()
        data class Error(val message: String) : OrderState()
    }

    data class Order(
        val id: String,
        val orderCode: String,
        val totalAmount: Double,
        val status: String,
        val items: List<OrderItem>
    )

    data class OrderItem(
        val name: String,
        val quantity: Int,
        val price: Int
    )
} 