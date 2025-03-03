package com.example.commeradeswallet

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.ktx.Firebase
import com.google.firebase.ktx.initialize

class CommeradesWalletApp : Application() {
    
    companion object {
        private lateinit var instance: CommeradesWalletApp
        @Volatile
        private var isFirebaseInitialized = false
        
        fun getInstance(): CommeradesWalletApp = instance
        fun isFirebaseReady() = isFirebaseInitialized
    }
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        
        // Initialize Firebase synchronously
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
                Firebase.initialize(this)
            }
            isFirebaseInitialized = true
        } catch (e: Exception) {
            Log.e("CommeradesWallet", "Error initializing Firebase", e)
            throw e // Fail fast if Firebase can't be initialized
        }
        
        // Set up global exception handler
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("CommeradesWallet", "Uncaught exception in thread $thread", throwable)
        }
    }
} 