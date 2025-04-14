package com.example.commeradeswallet.ui.screens.wallet

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.commeradeswallet.data.mpesa.DarajaClient
import com.example.commeradeswallet.data.mpesa.DarajaRepository
import com.example.commeradeswallet.data.repository.MpesaRepository
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class TopUpViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TopUpViewModel::class.java)) {
            // Initialize Retrofit for Daraja API
            val retrofit = Retrofit.Builder()
                .baseUrl("https://sandbox.safaricom.co.ke/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val darajaClient = retrofit.create(DarajaClient::class.java)
            
            // Initialize DarajaRepository with required parameters
            val darajaRepository = DarajaRepository(
                client = darajaClient,
                consumerKey = "i0Ci7KCr11HyeGDaVYfPbGE7ZcoYiyxsES4SlBabyCFgHGf3",
                consumerSecret = "wvQsxgvlaFvvJu7sGw2rzIIiWXC5GwSIwMpSq7VWRRwkYx0kbs05OGhsTR2C3Wc7",
                passKey = "MTc0Mzc5YmZiMjc5ZjlhYTliZGJjZjE1OGU5N2RkNzFhNDY3Y2QyZTBjODkzMDU5YjEwZjc4ZTZiNzJhZGExZWQyYzkxOTIwMjUwNDA2MDkyOTQ3",
                businessShortCode = "174379",
                callbackUrl = "https://mydomain.com/path"
            )
            
            val mpesaRepository = MpesaRepository(darajaRepository)
            return TopUpViewModel(mpesaRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
} 