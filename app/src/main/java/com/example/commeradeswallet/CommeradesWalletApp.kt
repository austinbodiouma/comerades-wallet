package com.example.commeradeswallet

import android.app.Application
import android.os.Build
import android.util.Log
import com.example.commeradeswallet.data.AppDatabase
import com.example.commeradeswallet.data.FirestoreInitializer
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.ktx.messaging
import com.google.firebase.ktx.Firebase
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
            
            // Initialize Firestore data
            firestoreInitializer = FirestoreInitializer(this)
            
            // Initialize FCM
            initializeFirebaseMessaging()

            // Initialize Auth
            FirebaseAuth.getInstance().addAuthStateListener { auth ->
                auth.currentUser?.let { user ->
                    Log.d("CommeradesWalletApp", "User signed in: ${user.uid}")
                } ?: Log.d("CommeradesWalletApp", "User signed out")
            }

            Log.d("CommeradesWalletApp", "Firebase services initialized successfully")
        } catch (e: Exception) {
            Log.e("CommeradesWalletApp", "Failed to initialize Firebase services", e)
        }
    }

    private fun initializeFirestoreData() {
        firestoreInitializer.initializeData()
        
        // Create test data if running on Android O or higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CoroutineScope(Dispatchers.IO).launch {
                firestoreInitializer.createTestData(
                    email = "test@example.com",
                    name = "Test User"
                )
            }
        }
    }

    private fun initializeFirebaseMessaging() {
        Firebase.messaging.token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("CommeradesWalletApp", "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }

            // Get new FCM registration token
            val token = task.result
            Log.d("CommeradesWalletApp", "FCM Token: $token")
            // Initialize Firestore data after Firebase is ready
            initializeFirestoreData()
        }

        // Subscribe to topics if needed
        FirebaseMessaging.getInstance().subscribeToTopic("general")
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("CommeradesWalletApp", "Successfully subscribed to topic: general")
                } else {
                    Log.w("CommeradesWalletApp", "Failed to subscribe to topic: general", task.exception)
                }
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