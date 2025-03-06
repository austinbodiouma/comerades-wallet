package com.example.commeradeswallet

import android.app.Application
import android.util.Log
import com.example.commeradeswallet.data.AppDatabase
import com.example.commeradeswallet.data.FirestoreInitializer
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CommeradesWalletApp : Application() {
    lateinit var database: AppDatabase
        private set
    
    private lateinit var firestoreInitializer: FirestoreInitializer

    override fun onCreate() {
        super.onCreate()
        Log.d("CommeradesWalletApp", "Initializing application")
        
        // Initialize Room database
        database = AppDatabase.getDatabase(this)
        
        // Initialize Firebase
        try {
            FirebaseApp.initializeApp(this)
            
            // Configure Firestore
            val db = FirebaseFirestore.getInstance()
            val settings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                .build()
            db.firestoreSettings = settings
            
            // Initialize FCM
            FirebaseMessaging.getInstance().token
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val token = task.result
                        Log.d("CommeradesWalletApp", "FCM Token: $token")
                    } else {
                        Log.e("CommeradesWalletApp", "Failed to get FCM token", task.exception)
                    }
                }

            // Initialize Auth
            FirebaseAuth.getInstance().addAuthStateListener { auth ->
                auth.currentUser?.let { user ->
                    Log.d("CommeradesWalletApp", "User signed in: ${user.uid}")
                } ?: Log.d("CommeradesWalletApp", "User signed out")
            }

            // Initialize Firestore data
            firestoreInitializer = FirestoreInitializer(this)
            initializeFirestoreData()

            Log.d("CommeradesWalletApp", "Firebase services initialized successfully")
        } catch (e: Exception) {
            Log.e("CommeradesWalletApp", "Failed to initialize Firebase services", e)
        }
    }

    private fun initializeFirestoreData() {
        firestoreInitializer.initializeData()
        
        // Optionally create test data (comment out in production)
        CoroutineScope(Dispatchers.IO).launch {
            firestoreInitializer.createTestData(
                email = "test@example.com",
                name = "Test User"
            )
        }
    }

    companion object {
        private var instance: CommeradesWalletApp? = null

        fun getInstance(): CommeradesWalletApp {
            return instance ?: throw IllegalStateException("Application not initialized")
        }
    }

    init {
        instance = this
    }
}