package com.example.commeradeswallet.data

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.commeradeswallet.data.dao.FoodDao
import com.example.commeradeswallet.data.dao.OrderDao
import com.example.commeradeswallet.data.dao.UserDao
import com.example.commeradeswallet.data.dao.WalletDao
import com.example.commeradeswallet.data.model.FoodItem
import com.example.commeradeswallet.data.model.Order
import com.example.commeradeswallet.data.model.User
import com.example.commeradeswallet.data.model.WalletTransaction
import com.example.commeradeswallet.data.converter.Converters

@Database(
    entities = [
        FoodItem::class,
        Order::class,
        User::class,
        WalletTransaction::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun foodDao(): FoodDao
    abstract fun orderDao(): OrderDao
    abstract fun userDao(): UserDao
    abstract fun walletDao(): WalletDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                try {
                    val instance = Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "app_database"
                    )
                        .fallbackToDestructiveMigration()
                        .build()
                    INSTANCE = instance
                    instance
                } catch (e: Exception) {
                    Log.e("AppDatabase", "Error creating database", e)
                    throw e
                }
            }
        }
    }
} 