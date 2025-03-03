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

class DashboardViewModel(
    private val repository: FoodRepository
) : ViewModel() {
    
    private val _foodItems = MutableStateFlow<List<FoodItem>>(emptyList())
    val foodItems: StateFlow<List<FoodItem>> = _foodItems.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                repository.getAllFoodItems().collect { items ->
                    Log.d("DashboardViewModel", "Loaded ${items.size} food items: $items")
                    _foodItems.value = items
                }
            } catch (e: Exception) {
                Log.e("DashboardViewModel", "Error loading food items", e)
                _error.value = e.message ?: "Unknown error occurred"
            }
        }
    }

    fun searchFoodItems(query: String) {
        viewModelScope.launch {
            repository.searchFoodItems(query).collect { items ->
                _foodItems.value = items
            }
        }
    }

    fun filterByCategory(category: String) {
        viewModelScope.launch {
            if (category == "All") {
                repository.getAllFoodItems().collect { items ->
                    _foodItems.value = items
                }
            } else {
                repository.getFoodItemsByCategory(category).collect { items ->
                    _foodItems.value = items
                }
            }
        }
    }

    fun updateQuantity(foodItem: FoodItem, quantity: Int) {
        // Implement order logic here
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