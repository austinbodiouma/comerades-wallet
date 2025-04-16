package com.example.commeradeswallet.ui.screens.dashboard

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.commeradeswallet.data.AppDatabase
import com.example.commeradeswallet.data.model.FoodItem
import com.example.commeradeswallet.data.repository.FoodRepository
import com.example.commeradeswallet.data.repository.PreviewFoodRepository
import com.example.commeradeswallet.data.repository.RoomFoodRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

class DashboardViewModel(
    private val repository: FoodRepository
) : ViewModel() {
    
    private val _foodItems = MutableStateFlow<List<FoodItem>>(emptyList())
    val foodItems: StateFlow<List<FoodItem>> = _foodItems.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var currentCategory: String = "All"
    private var currentSearchQuery: String = ""

    init {
        Log.d("DashboardViewModel", "Initializing ViewModel")
        loadFoodItems()
    }

    private fun loadFoodItems() {
        viewModelScope.launch {
            try {
                repository.getAllFoodItems().collect { items ->
                    // Process items to ensure uniqueness and filter unavailable items
                    val uniqueItems = items
                        .distinctBy { it.id }
                        .filter { it.isAvailable }
                    
                    Log.d("DashboardViewModel", "Loaded ${items.size} food items, ${uniqueItems.size} unique available items")
                    _foodItems.value = uniqueItems
                }
            } catch (e: Exception) {
                Log.e("DashboardViewModel", "Error loading food items", e)
                _error.value = e.message ?: "Unknown error occurred"
            }
        }
    }

    fun searchFoodItems(query: String) {
        currentSearchQuery = query
        viewModelScope.launch {
            try {
                repository.searchFoodItems(query).collect { items ->
                    // Process items to ensure uniqueness and filter unavailable items
                    val uniqueItems = items
                        .distinctBy { it.id }
                        .filter { it.isAvailable }
                    
                    _foodItems.value = uniqueItems
                }
            } catch (e: Exception) {
                Log.e("DashboardViewModel", "Error searching food items", e)
                _error.value = e.message ?: "Error searching food items"
            }
        }
    }

    fun filterByCategory(category: String) {
        currentCategory = category
        viewModelScope.launch {
            try {
                if (category == "All") {
                    repository.getAllFoodItems().collect { items ->
                        // Process items to ensure uniqueness and filter unavailable items
                        val uniqueItems = items
                            .distinctBy { it.id }
                            .filter { it.isAvailable }
                        
                        _foodItems.value = uniqueItems
                    }
                } else {
                    repository.getFoodItemsByCategory(category).collect { items ->
                        // Process items to ensure uniqueness and filter unavailable items
                        val uniqueItems = items
                            .distinctBy { it.id }
                            .filter { it.isAvailable }
                        
                        _foodItems.value = uniqueItems
                    }
                }
            } catch (e: Exception) {
                Log.e("DashboardViewModel", "Error filtering by category", e)
                _error.value = e.message ?: "Error filtering by category"
            }
        }
    }

    fun updateQuantity(foodItem: FoodItem, quantity: Int) {
        // Implement order logic here
    }

    fun refreshFoodItems() {
        viewModelScope.launch {
            try {
                _isRefreshing.value = true
                Log.d("DashboardViewModel", "Manual refresh triggered")
                
                // Refresh data from server
                repository.refreshFoodItems()
                
                // Wait a moment for DB to update
                delay(300)
                
                // Re-apply current filters after refresh
                if (currentSearchQuery.isNotEmpty()) {
                    searchFoodItems(currentSearchQuery)
                } else {
                    filterByCategory(currentCategory)
                }
            } catch (e: Exception) {
                Log.e("DashboardViewModel", "Error refreshing food items", e)
                _error.value = "Failed to refresh: ${e.message}"
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = checkNotNull(get(APPLICATION_KEY))
                val repository = RoomFoodRepository(AppDatabase.getDatabase(application).foodDao())
                DashboardViewModel(repository)
            }
        }

        fun previewViewModel(): DashboardViewModel {
            return DashboardViewModel(PreviewFoodRepository())
        }
    }
} 