package com.example.commeradeswallet.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class AuthRepository {
    private val TAG = "AuthRepository"
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    suspend fun signIn(email: String, password: String): Result<FirebaseUser> = suspendCoroutine { continuation ->
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                result.user?.let { user ->
                    Log.d(TAG, "User sign in successful: ${user.uid}")
                    continuation.resume(Result.success(user))
                } ?: continuation.resume(Result.failure(Exception("Authentication failed")))
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "User sign in failed", e)
                continuation.resume(Result.failure(e))
            }
    }

    suspend fun signUp(
        email: String,
        password: String,
        name: String,
        studentId: String,
        phoneNumber: String
    ): Result<FirebaseUser> = suspendCoroutine { continuation ->
        Log.d(TAG, "Attempting to create user: $email")
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                result.user?.let { user ->
                    Log.d(TAG, "User created successfully: ${user.uid}")
                    // Create user profile in Firestore
                    val userProfile = hashMapOf(
                        "name" to name,
                        "email" to email,
                        "studentId" to studentId,
                        "phoneNumber" to phoneNumber,
                        "createdAt" to com.google.firebase.Timestamp.now()
                    )

                    // Create user profile document
                    firestore.collection("users")
                        .document(user.uid)
                        .set(userProfile)
                        .addOnSuccessListener {
                            Log.d(TAG, "User profile created successfully")
                            
                            // Initialize wallet with zero balance
                            val wallet = hashMapOf(
                                "balance" to 0.0,
                                "createdAt" to com.google.firebase.Timestamp.now(),
                                "currency" to "KES"
                            )
                            
                            // Create wallet document
                            firestore.collection("wallets")
                                .document(user.uid)
                                .set(wallet)
                                .addOnSuccessListener {
                                    Log.d(TAG, "Wallet initialized successfully")
                                    continuation.resume(Result.success(user))
                                }
                                .addOnFailureListener { e ->
                                    Log.e(TAG, "Failed to initialize wallet", e)
                                    // We don't delete the user here as the profile was created successfully
                                    // We can retry wallet creation later
                                    continuation.resume(Result.success(user))
                                }
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Failed to create user profile", e)
                            // If profile creation fails, delete the auth user
                            user.delete()
                                .addOnCompleteListener {
                                    Log.d(TAG, "Deleted auth user after profile creation failure")
                                    continuation.resume(Result.failure(e))
                                }
                        }
                } ?: continuation.resume(Result.failure(Exception("User creation failed")))
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to create user", e)
                continuation.resume(Result.failure(e))
            }
    }

    suspend fun resetPassword(email: String): Result<Unit> = suspendCoroutine { continuation ->
        auth.sendPasswordResetEmail(email)
            .addOnSuccessListener {
                Log.d(TAG, "Password reset email sent to: $email")
                continuation.resume(Result.success(Unit))
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to send password reset email", e)
                continuation.resume(Result.failure(e))
            }
    }

    suspend fun getUserProfile(): Result<Map<String, Any>> = suspendCoroutine { continuation ->
        currentUser?.let { user ->
            firestore.collection("users")
                .document(user.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        Log.d(TAG, "Successfully retrieved user profile")
                        continuation.resume(Result.success(document.data ?: emptyMap()))
                    } else {
                        Log.w(TAG, "User profile not found")
                        continuation.resume(Result.failure(Exception("User profile not found")))
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error retrieving user profile", e)
                    continuation.resume(Result.failure(e))
                }
        } ?: continuation.resume(Result.failure(Exception("User not authenticated")))
    }
    
    suspend fun getUserWallet(): Result<Map<String, Any>> = suspendCoroutine { continuation ->
        currentUser?.let { user ->
            firestore.collection("wallets")
                .document(user.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        Log.d(TAG, "Successfully retrieved user wallet")
                        continuation.resume(Result.success(document.data ?: emptyMap()))
                    } else {
                        Log.w(TAG, "User wallet not found")
                        continuation.resume(Result.failure(Exception("User wallet not found")))
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error retrieving user wallet", e)
                    continuation.resume(Result.failure(e))
                }
        } ?: continuation.resume(Result.failure(Exception("User not authenticated")))
    }

    fun signOut() {
        auth.signOut()
        Log.d(TAG, "User signed out")
    }
} 