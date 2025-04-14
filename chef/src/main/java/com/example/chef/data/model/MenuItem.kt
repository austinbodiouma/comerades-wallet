package com.example.chef.data.model

data class MenuItem(
    var id: String = "",
    val name: String = "",
    val price: Int = 0,
    val category: String = "",
    val imageUrl: String = "",
    val description: String = "",
    val isQuantifiedByNumber: Boolean = false,
    val isAvailable: Boolean = true,
    val lastUpdated: Long = System.currentTimeMillis()
) 