package com.example.cashier

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings

class CashierApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        try {
            // Initialize Firebase
            FirebaseApp.initializeApp(this)
            
            // Configure Firestore
            val db = FirebaseFirestore.getInstance()
            val settings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                .build()
            db.firestoreSettings = settings
            
            // Initialize Auth
            FirebaseAuth.getInstance().addAuthStateListener { auth ->
                auth.currentUser?.let { user ->
                    Log.d("CashierApp", "Cashier signed in: ${user.uid}")
                } ?: Log.d("CashierApp", "Cashier signed out")
            }

            Log.d("CashierApp", "Firebase services initialized successfully")
        } catch (e: Exception) {
            Log.e("CashierApp", "Failed to initialize Firebase services", e)
        }
    }
} 