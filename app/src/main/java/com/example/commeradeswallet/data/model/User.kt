package com.example.commeradeswallet.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey
    val id: String = "",
    val email: String,
    val password: String, // This should be hashed
    val name: String? = null,
    val phoneNumber: String? = null,
    val authProvider: String = "EMAIL", // EMAIL or GOOGLE
    val googleUserId: String? = null
) 