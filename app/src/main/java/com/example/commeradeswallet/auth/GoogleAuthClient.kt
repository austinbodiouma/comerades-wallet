package com.example.commeradeswallet.auth

import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.util.Log
import com.example.commeradeswallet.CommeradesWalletApp
import com.example.commeradeswallet.R
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.auth.api.identity.Identity
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import java.util.concurrent.CancellationException
import com.google.firebase.initialize

class GoogleAuthClient(
    private val context: Context
) {
    private val oneTapClient: SignInClient = Identity.getSignInClient(context)
    private var auth: FirebaseAuth? = null

    init {
        try {
            Log.d("GoogleAuthClient", "Initializing Firebase Auth")
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }
            auth = Firebase.auth
            Log.d("GoogleAuthClient", "Firebase Auth initialized successfully")
        } catch (e: Exception) {
            Log.e("GoogleAuthClient", "Failed to initialize Firebase Auth", e)
        }
    }

    fun getSignedInUser(): UserData? {
        if (auth == null) {
            try {
                if (FirebaseApp.getApps(context).isEmpty()) {
                    FirebaseApp.initializeApp(context)
                }
                auth = Firebase.auth
            } catch (e: Exception) {
                Log.e("GoogleAuthClient", "Failed to initialize Firebase Auth", e)
                return null
            }
        }
        return auth?.currentUser?.run {
            UserData(
                userId = uid,
                username = displayName ?: "",
                email = email ?: "",
                profilePictureUrl = photoUrl?.toString()
            )
        }
    }

    suspend fun signIn(): IntentSender? {
        try {
            Log.d("GoogleAuthClient", "Starting sign in process")
            val result = oneTapClient.beginSignIn(
                buildSignInRequest()
            ).await()
            Log.d("GoogleAuthClient", "Sign in intent created successfully")
            return result?.pendingIntent?.intentSender
        } catch (e: Exception) {
            Log.e("GoogleAuthClient", "Error during sign in", e)
            if (e is CancellationException) throw e
            return null
        }
    }

    suspend fun signInWithIntent(intent: Intent): SignInResult {
        try {
            Log.d("GoogleAuthClient", "Processing sign in intent")
            val credential = oneTapClient.getSignInCredentialFromIntent(intent)
            val googleIdToken = credential.googleIdToken
            
            if (googleIdToken == null) {
                Log.e("GoogleAuthClient", "No Google ID token found in credential")
                return SignInResult(
                    data = null,
                    errorMessage = "No token received"
                )
            }

            Log.d("GoogleAuthClient", "Got Google ID token, authenticating with Firebase")
            val googleCredentials = GoogleAuthProvider.getCredential(googleIdToken, null)
            val result = auth?.signInWithCredential(googleCredentials)?.await()
                ?: return SignInResult(
                    data = null,
                    errorMessage = "Sign in failed - auth is null"
                )

            Log.d("GoogleAuthClient", "Firebase authentication successful")
            return SignInResult(
                data = result.user?.run {
                    UserData(
                        userId = uid,
                        username = displayName ?: "",
                        email = email ?: "",
                        profilePictureUrl = photoUrl?.toString()
                    )
                },
                errorMessage = null
            )
        } catch (e: Exception) {
            Log.e("GoogleAuthClient", "Error processing sign in intent", e)
            if (e is CancellationException) throw e
            return SignInResult(
                data = null,
                errorMessage = e.message
            )
        }
    }

    suspend fun signOut() {
        try {
            oneTapClient.signOut().await()
            auth?.signOut()
        } catch(e: Exception) {
            e.printStackTrace()
            if(e is CancellationException) throw e
        }
    }

    private fun buildSignInRequest(): BeginSignInRequest {
        Log.d("GoogleAuthClient", "Building sign in request with web client ID: ${context.getString(R.string.web_client_id)}")
        return BeginSignInRequest.Builder()
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(context.getString(R.string.web_client_id))
                    .build()
            )
            .setAutoSelectEnabled(true)
            .build()
    }
} 