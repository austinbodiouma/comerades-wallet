package com.example.commeradeswallet.repository

import android.util.Log
import com.example.commeradeswallet.data.model.User
import com.example.commeradeswallet.data.repository.FirestoreRepository

class UserRepository(
    private val firestoreRepository: FirestoreRepository = FirestoreRepository()
) {
    suspend fun getUser(userId: String): Result<User> = try {
        firestoreRepository.getUser(userId).onFailure { 
            Log.e("UserRepository", "Error getting user with ID: $userId", it)
        }
    } catch (e: Exception) {
        Log.e("UserRepository", "Error retrieving user", e)
        Result.failure(e)
    }

    suspend fun createUser(user: User): Result<String> = try {
        firestoreRepository.createUser(user).onFailure { 
            Log.e("UserRepository", "Error creating user", it)
        }
    } catch (e: Exception) {
        Log.e("UserRepository", "Error creating user", e)
        Result.failure(e)
    }

    suspend fun updateUser(user: User): Result<Unit> = try {
        // Implementation will be added later
        Result.failure(Exception("Not implemented"))
    } catch (e: Exception) {
        Log.e("UserRepository", "Error updating user", e)
        Result.failure(e)
    }
}
