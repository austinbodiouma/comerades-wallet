package com.example.commeradeswallet.repository

import com.example.commeradeswallet.data.model.User
import com.example.commeradeswallet.data.repository.FirestoreRepository

class UserRepository(
    private val firestoreRepository: FirestoreRepository = FirestoreRepository()
) {
    suspend fun getUser(userId: String): Result<User> {
        return firestoreRepository.getUser(userId)
    }

    suspend fun createUser(user: User): Result<String> {
        return firestoreRepository.createUser(user)
    }

    suspend fun updateUser(user: User): Result<Unit> {
        // Implementation will be added later
        return Result.failure(Exception("Not implemented"))
    }
} 