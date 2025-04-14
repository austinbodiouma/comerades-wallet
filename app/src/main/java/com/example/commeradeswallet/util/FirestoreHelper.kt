package com.example.commeradeswallet.util

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object FirestoreHelper {
    private const val TAG = "FirestoreHelper"
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    /**
     * Verify if the currently logged in user has a profile in Firestore
     * @return true if the user has a profile, false otherwise
     */
    suspend fun verifyUserHasProfile(): Boolean {
        val currentUser = auth.currentUser ?: return false
        
        return try {
            val documentSnapshot = firestore.collection("users")
                .document(currentUser.uid)
                .get()
                .await()
            
            val exists = documentSnapshot.exists()
            Log.d(TAG, "User profile exists: $exists")
            
            if (exists) {
                // Log the profile data for debugging
                val data = documentSnapshot.data
                Log.d(TAG, "User profile data: $data")
            }
            
            exists
        } catch (e: Exception) {
            Log.e(TAG, "Error verifying user profile", e)
            false
        }
    }
    
    /**
     * Verify if the currently logged in user has a wallet in Firestore
     * @return true if the user has a wallet, false otherwise
     */
    suspend fun verifyUserHasWallet(): Boolean {
        val currentUser = auth.currentUser ?: return false
        
        return try {
            val documentSnapshot = firestore.collection("wallets")
                .document(currentUser.uid)
                .get()
                .await()
            
            val exists = documentSnapshot.exists()
            Log.d(TAG, "User wallet exists: $exists")
            
            if (exists) {
                // Log the wallet data for debugging
                val data = documentSnapshot.data
                Log.d(TAG, "User wallet data: $data")
            }
            
            exists
        } catch (e: Exception) {
            Log.e(TAG, "Error verifying user wallet", e)
            false
        }
    }
    
    /**
     * Verify that both profile and wallet exist for the current user
     * @return true if both exist, false otherwise
     */
    suspend fun verifyUserData(): Boolean {
        val hasProfile = verifyUserHasProfile()
        val hasWallet = verifyUserHasWallet()
        
        Log.d(TAG, "User data verification: profile=$hasProfile, wallet=$hasWallet")
        
        return hasProfile && hasWallet
    }
    
    /**
     * Get the fields in the user's profile
     * @return Map of fields or null if user is not logged in or profile doesn't exist
     */
    suspend fun getUserProfileFields(): Map<String, Any>? {
        val currentUser = auth.currentUser ?: return null
        
        return try {
            val documentSnapshot = firestore.collection("users")
                .document(currentUser.uid)
                .get()
                .await()
            
            if (documentSnapshot.exists()) {
                documentSnapshot.data
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user profile fields", e)
            null
        }
    }
    
    /**
     * Get the fields in the user's wallet
     * @return Map of fields or null if user is not logged in or wallet doesn't exist
     */
    suspend fun getUserWalletFields(): Map<String, Any>? {
        val currentUser = auth.currentUser ?: return null
        
        return try {
            val documentSnapshot = firestore.collection("wallets")
                .document(currentUser.uid)
                .get()
                .await()
            
            if (documentSnapshot.exists()) {
                documentSnapshot.data
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user wallet fields", e)
            null
        }
    }
} 