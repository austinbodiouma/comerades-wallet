package com.example.chef.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.chef.data.model.MenuItem
import com.example.chef.data.repository.MenuRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class InventoryViewModel(
    private val menuRepository: MenuRepository
) : ViewModel() {

    private val _menuItemsState = MutableStateFlow<MenuItemsState>(MenuItemsState.Loading)
    val state: StateFlow<MenuItemsState> = _menuItemsState
    private val firestore = FirebaseFirestore.getInstance()

    init {
        initializeDataIfNeeded()
    }

    private fun initializeDataIfNeeded() {
        viewModelScope.launch {
            try {
                // Check if we need to add default categories
                menuRepository.addDefaultCategories()
                // Then load menu items
                loadMenuItems()
            } catch (e: Exception) {
                Log.e("InventoryViewModel", "Error initializing data: ${e.message}", e)
                loadMenuItems() // Still try to load items even if defaults fail
            }
        }
    }

    fun loadMenuItems() {
        viewModelScope.launch {
            try {
                _menuItemsState.value = MenuItemsState.Loading
                
                // Load all menu items
                val menuItems = menuRepository.getMenuItems()
                
                if (menuItems.isEmpty()) {
                    _menuItemsState.value = MenuItemsState.Empty
                } else {
                    _menuItemsState.value = MenuItemsState.Success(menuItems)
                }
            } catch (e: Exception) {
                Log.e("InventoryViewModel", "Error loading menu items: ${e.message}", e)
                _menuItemsState.value = MenuItemsState.Error("Failed to load menu items: ${e.message}")
            }
        }
    }

    fun toggleItemAvailability(item: MenuItem) {
        val newAvailability = !item.isAvailable
        Log.d("InventoryViewModel", "Toggling availability for ${item.name} (${item.id}) to $newAvailability")
        
        viewModelScope.launch {
            try {
                // First, update the local state with optimistic UI update
                updateLocalItemState(item.id, newAvailability)
                
                // Then update Firestore through repository
                menuRepository.updateItemAvailability(item.id, newAvailability)
                
                Log.d("InventoryViewModel", "Successfully toggled item availability for ${item.name}")
            } catch (e: Exception) {
                Log.e("InventoryViewModel", "Error toggling item availability: ${e.message}", e)
                
                // Revert local state if Firestore update failed
                updateLocalItemState(item.id, item.isAvailable)
                
                // Additionally refresh the list to ensure UI consistency with server state
                loadMenuItems()
            }
        }
    }

    private fun updateLocalItemState(itemId: String, isAvailable: Boolean) {
        val currentItems = (_menuItemsState.value as? MenuItemsState.Success)?.items ?: listOf()
        val updatedItems = currentItems.map { 
            if (it.id == itemId) it.copy(isAvailable = isAvailable) else it 
        }
        _menuItemsState.value = MenuItemsState.Success(updatedItems)
    }

    sealed class MenuItemsState {
        object Loading : MenuItemsState()
        data class Success(val items: List<MenuItem>) : MenuItemsState()
        data class Error(val message: String) : MenuItemsState()
        object Empty : MenuItemsState()
    }

    class Factory(private val menuRepository: MenuRepository = MenuRepository()) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(InventoryViewModel::class.java)) {
                return InventoryViewModel(menuRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
} 