package com.example.commeradeswallet.ui.viewmodel

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.commeradeswallet.data.dao.FoodDao
import com.example.commeradeswallet.data.dao.OrderDao
import com.example.commeradeswallet.data.dao.WalletDao
import com.example.commeradeswallet.data.model.*
import com.example.commeradeswallet.data.repository.FirestoreRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class CashierViewModel(
    private val orderDao: OrderDao,
    private val foodDao: FoodDao,
    private val walletDao: WalletDao,
    private val firestoreRepository: FirestoreRepository
) : ViewModel() {

    private val _orderState = MutableStateFlow<OrderState>(OrderState.Loading)
    val orderState: StateFlow<OrderState> = _orderState.asStateFlow()

    private val _menuState = MutableStateFlow<MenuState>(MenuState.Loading)
    val menuState: StateFlow<MenuState> = _menuState.asStateFlow()

    private val _transactionState = MutableStateFlow<TransactionState>(TransactionState.Loading)
    val transactionState: StateFlow<TransactionState> = _transactionState.asStateFlow()

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _verificationState = MutableStateFlow<VerificationState>(VerificationState.Idle)
    val verificationState: StateFlow<VerificationState> = _verificationState.asStateFlow()

    init {
        loadOrders()
        loadMenuItems()
        loadTransactions()
    }

    private fun loadOrders() {
        viewModelScope.launch {
            try {
                orderDao.getAllOrders()
                    .catch { e ->
                        Log.e("CashierViewModel", "Error loading orders from Room", e)
                        _orderState.value = OrderState.Error("Failed to load orders: ${e.message}")
                    }
                    .collect { localOrders ->
                        if (localOrders.isEmpty()) {
                            fetchOrdersFromFirestore()
                        } else {
                            _orderState.value = OrderState.Success(localOrders)
                        }
                    }
            } catch (e: Exception) {
                Log.e("CashierViewModel", "Error in loadOrders", e)
                _orderState.value = OrderState.Error("Failed to load orders: ${e.message}")
            }
        }
    }

    private fun loadMenuItems() {
        viewModelScope.launch {
            try {
                foodDao.getAllFoodItems()
                    .catch { e ->
                        Log.e("CashierViewModel", "Error loading menu items", e)
                        _menuState.value = MenuState.Error("Failed to load menu items: ${e.message}")
                    }
                    .collect { items ->
                        _menuState.value = MenuState.Success(items)
                    }
            } catch (e: Exception) {
                Log.e("CashierViewModel", "Error in loadMenuItems", e)
                _menuState.value = MenuState.Error("Failed to load menu items: ${e.message}")
            }
        }
    }

    private suspend fun fetchOrdersFromFirestore() {
        try {
            firestoreRepository.getOrders()
                .onSuccess { orders ->
                    viewModelScope.launch {
                        orders.forEach { order ->
                            orderDao.insertOrder(order)
                        }
                        _orderState.value = OrderState.Success(orders)
                    }
                }
                .onFailure { e ->
                    Log.e("CashierViewModel", "Error fetching orders from Firestore", e)
                    _orderState.value = OrderState.Error("Failed to fetch orders: ${e.message}")
                }
        } catch (e: Exception) {
            Log.e("CashierViewModel", "Error in fetchOrdersFromFirestore", e)
            _orderState.value = OrderState.Error("Failed to fetch orders: ${e.message}")
        }
    }

    private suspend fun fetchMenuItemsFromFirestore() {
        try {
            firestoreRepository.getAllFoodItems()
                .onSuccess { items ->
                    viewModelScope.launch {
                        foodDao.insertAll(items)
                        _menuState.value = MenuState.Success(items)
                    }
                }
                .onFailure { e ->
                    Log.e("CashierViewModel", "Error fetching menu items from Firestore", e)
                    _menuState.value = MenuState.Error("Failed to fetch menu items: ${e.message}")
                }
        } catch (e: Exception) {
            Log.e("CashierViewModel", "Error in fetchMenuItemsFromFirestore", e)
            _menuState.value = MenuState.Error("Failed to fetch menu items: ${e.message}")
        }
    }

    private fun loadTransactions() {
        viewModelScope.launch {
            _transactionState.value = TransactionState.Loading
            try {
                firestoreRepository.getAllTransactions().fold(
                    onSuccess = { transactions ->
                        _transactionState.value = TransactionState.Success(transactions)
                    },
                    onFailure = { exception ->
                        _transactionState.value = TransactionState.Error(exception.message ?: "Unknown error")
                    }
                )
            } catch (e: Exception) {
                _transactionState.value = TransactionState.Error(e.message ?: "Unknown error")
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun updateOrderStatus(order: Order, newStatus: OrderStatus) {
        viewModelScope.launch {
            try {
                val updatedOrder = order.copy(
                    status = newStatus,
                    timestamp = LocalDateTime.now()
                )

                firestoreRepository.updateOrder(updatedOrder)
                    .onSuccess {
                        orderDao.updateOrder(updatedOrder)
                        loadOrders()
                        
                        if (newStatus == OrderStatus.COMPLETED) {
                            firestoreRepository.processPayment(updatedOrder)
                        }
                    }
                    .onFailure { e ->
                        Log.e("CashierViewModel", "Error updating order in Firestore", e)
                        _orderState.value = OrderState.Error("Failed to update order: ${e.message}")
                    }
            } catch (e: Exception) {
                Log.e("CashierViewModel", "Error in updateOrderStatus", e)
                _orderState.value = OrderState.Error("Failed to update order: ${e.message}")
            }
        }
    }

    fun addMenuItem(item: FoodItem) {
        viewModelScope.launch {
            try {
                firestoreRepository.addFoodItem(item)
                    .onSuccess { id ->
                        val newItem = item.copy(id = id)
                        foodDao.insert(newItem)
                        loadMenuItems()
                    }
                    .onFailure { e ->
                        Log.e("CashierViewModel", "Error adding menu item to Firestore", e)
                        _menuState.value = MenuState.Error("Failed to add menu item: ${e.message}")
                    }
            } catch (e: Exception) {
                Log.e("CashierViewModel", "Error in addMenuItem", e)
                _menuState.value = MenuState.Error("Failed to add menu item: ${e.message}")
            }
        }
    }

    fun updateMenuItem(item: FoodItem) {
        viewModelScope.launch {
            try {
                firestoreRepository.updateFoodItem(item)
                    .onSuccess {
                        foodDao.update(item)
                        loadMenuItems()
                    }
                    .onFailure { e ->
                        Log.e("CashierViewModel", "Error updating menu item in Firestore", e)
                        _menuState.value = MenuState.Error("Failed to update menu item: ${e.message}")
                    }
            } catch (e: Exception) {
                Log.e("CashierViewModel", "Error in updateMenuItem", e)
                _menuState.value = MenuState.Error("Failed to update menu item: ${e.message}")
            }
        }
    }

    fun setSelectedTab(index: Int) {
        _selectedTab.value = index
    }

    fun verifyOrderByCode(orderCode: String) {
        viewModelScope.launch {
            _verificationState.value = VerificationState.Loading
            try {
                orderDao.getAllOrders()
                    .first()
                    .find { it.orderCode == orderCode }
                    ?.let { order ->
                        if (order.status == OrderStatus.PENDING) {
                            _verificationState.value = VerificationState.Success(order)
                        } else {
                            _verificationState.value = VerificationState.Error(
                                "Order ${order.orderCode} is already ${order.status.name.lowercase()}"
                            )
                        }
                    } ?: run {
                        // If not found in local database, check Firestore
                        firestoreRepository.getOrderByCode(orderCode)
                            .onSuccess { order ->
                                if (order != null) {
                                    if (order.status == OrderStatus.PENDING) {
                                        orderDao.insertOrder(order)
                                        _verificationState.value = VerificationState.Success(order)
                                    } else {
                                        _verificationState.value = VerificationState.Error(
                                            "Order ${order.orderCode} is already ${order.status.name.lowercase()}"
                                        )
                                    }
                                } else {
                                    _verificationState.value = VerificationState.Error("Order not found")
                                }
                            }
                            .onFailure { e ->
                                _verificationState.value = VerificationState.Error(e.message ?: "Failed to verify order")
                            }
                    }
            } catch (e: Exception) {
                _verificationState.value = VerificationState.Error(e.message ?: "Failed to verify order")
            }
        }
    }

    fun resetVerificationState() {
        _verificationState.value = VerificationState.Idle
    }

    private fun generateOrderId(): String {
        return "ORD_${System.currentTimeMillis()}"
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun processOrder(cartItems: List<CartItem>, totalAmount: Double, userId: String) {
        viewModelScope.launch {
            try {
                val order = createOrder(cartItems, totalAmount, userId)
                // Process order logic here
            } catch (e: Exception) {
                Log.e("CashierViewModel", "Error processing order", e)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createOrder(cartItems: List<CartItem>, totalAmount: Double, userId: String): Order {
        return Order(
            id = generateOrderId(),
            userId = userId,
            items = cartItems,
            totalAmount = totalAmount,
            status = OrderStatus.PENDING,
            timestamp = LocalDateTime.now()
        )
    }

    sealed class OrderState {
        object Loading : OrderState()
        data class Success(val orders: List<Order>) : OrderState()
        data class Error(val message: String) : OrderState()
    }

    sealed class MenuState {
        object Loading : MenuState()
        data class Success(val items: List<FoodItem>) : MenuState()
        data class Error(val message: String) : MenuState()
    }

    sealed class TransactionState {
        object Loading : TransactionState()
        data class Success(val transactions: List<WalletTransaction>) : TransactionState()
        data class Error(val message: String) : TransactionState()
    }

    sealed class VerificationState {
        object Idle : VerificationState()
        object Loading : VerificationState()
        data class Success(val order: Order) : VerificationState()
        data class Error(val message: String) : VerificationState()
    }
} 
