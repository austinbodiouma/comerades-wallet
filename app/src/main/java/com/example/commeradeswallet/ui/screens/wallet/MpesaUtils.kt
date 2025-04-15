package com.example.commeradeswallet.ui.screens.wallet

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.commeradeswallet.data.mpesa.DarajaClient
import com.example.commeradeswallet.data.mpesa.DarajaRepository
import com.example.commeradeswallet.data.repository.MpesaRepository
import com.example.commeradeswallet.data.repository.MpesaTransactionRepository
import com.example.commeradeswallet.data.repository.WalletRepository
import com.example.commeradeswallet.ui.viewmodel.MpesaViewModel
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Creates and returns an instance of MpesaViewModel with all the necessary dependencies.
 * This function can be used from any @Composable where access to the MpesaViewModel is needed.
 */
@Composable
fun createMpesaViewModel(): MpesaViewModel {
    val context = LocalContext.current
    
    // Create HTTP client
    val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    
    val httpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .addInterceptor { chain ->
            val original = chain.request()
            val requestBuilder = original.newBuilder()
                .header("Accept", "application/json")
                .header("Cache-Control", "no-cache")
                .method(original.method, original.body)
            
            val request = requestBuilder.build()
            chain.proceed(request)
        }
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
    
    // Create Retrofit API client
    val retrofit = Retrofit.Builder()
        .baseUrl("https://sandbox.safaricom.co.ke/")
        .client(httpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    val darajaClient = retrofit.create(DarajaClient::class.java)
    
    // Create repositories
    val darajaRepository = DarajaRepository(
        client = darajaClient,
        consumerKey = "dWAFID9tiUC1oZ8GtIQ7kg0amPSDWJA6gDt1kSlBqjtchdqb",
        consumerSecret = "8pt2hm218OC3CmvhfG3MHtgJvJAzZf7nDlBfGu6BNI9LZNyIhTQwApvOTVRXxcqV",
        passKey = "bfb279f9aa9bdbcf158e97dd71a467cd2e0c893059b10f78e6b72ada1ed2c919",
        businessShortCode = "174379",
        callbackUrl = "https://mydomain.com/mpesa/callback"
    )
    
    val mpesaRepository = MpesaRepository(darajaRepository)
    val transactionRepository = MpesaTransactionRepository()
    val walletRepository = WalletRepository()
    
    return viewModel(
        factory = MpesaViewModel.Factory(mpesaRepository, transactionRepository, walletRepository)
    )
}

/**
 * Utility function to format a phone number to the M-Pesa format
 */
fun formatPhoneNumber(number: String): String {
    // Remove any spaces, dashes, or other characters
    val cleaned = number.replace(Regex("[^0-9]"), "")
    
    return when {
        // If starts with 254, use as is
        cleaned.startsWith("254") -> cleaned
        // If starts with 0, replace with 254
        cleaned.startsWith("0") -> "254${cleaned.substring(1)}"
        // If starts with 7 or 1, add 254
        cleaned.startsWith("7") || cleaned.startsWith("1") -> "254$cleaned"
        // Otherwise return as is
        else -> cleaned
    }
}

/**
 * Validates a phone number for M-Pesa
 */
fun validatePhoneNumber(number: String): Boolean {
    val formatted = formatPhoneNumber(number)
    return formatted.length == 12 && formatted.startsWith("254")
} 