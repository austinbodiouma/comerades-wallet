package com.example.chef

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.chef.navigation.ChefNavigation
import com.example.chef.ui.theme.ChefTheme
import com.google.firebase.FirebaseApp

class MainActivity : ComponentActivity() {
    private val TAG = "ChefMainActivity"
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            // Initialize Firebase if not already initialized
            if (!FirebaseApp.getApps(this).any()) {
                FirebaseApp.initializeApp(this)
                Log.d(TAG, "Firebase initialized successfully")
            } else {
                Log.d(TAG, "Firebase was already initialized")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Firebase", e)
        }
        
        Log.d(TAG, "Setting up UI with ChefTheme and ChefNavigation")
        setContent {
            ChefTheme {
                ChefNavigation()
            }
            Log.d(TAG, "UI setup completed successfully")
        }
    }
} 