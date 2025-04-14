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
import com.example.commeradeswallet.data.dao.StockDao
import com.example.commeradeswallet.data.model.FoodItem
import com.example.commeradeswallet.data.model.Order
import com.example.commeradeswallet.data.model.User
import com.example.commeradeswallet.data.model.WalletTransaction
import com.example.commeradeswallet.data.model.StockItem
import com.example.commeradeswallet.data.converter.Converters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        FoodItem::class,
        Order::class,
        User::class,
        WalletTransaction::class,
        StockItem::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun foodDao(): FoodDao
    abstract fun orderDao(): OrderDao
    abstract fun userDao(): UserDao
    abstract fun walletDao(): WalletDao
    abstract fun stockDao(): StockDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create the new users table with all required columns
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS users (
                        id TEXT PRIMARY KEY NOT NULL,
                        email TEXT NOT NULL,
                        password TEXT NOT NULL,
                        name TEXT,
                        phoneNumber TEXT,
                        authProvider TEXT NOT NULL DEFAULT 'EMAIL',
                        googleUserId TEXT
                    )
                """)
                database.execSQL("CREATE TABLE IF NOT EXISTS food_items (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, price INTEGER NOT NULL, category TEXT NOT NULL, imageUrl TEXT, description TEXT, isQuantifiedByNumber INTEGER NOT NULL DEFAULT 0, isAvailable INTEGER NOT NULL DEFAULT 1, lastUpdated INTEGER NOT NULL DEFAULT 0)")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // First, rename the old orders table
                database.execSQL("ALTER TABLE orders RENAME TO orders_old")

                // Create new orders table with TEXT id
                database.execSQL("""
                    CREATE TABLE orders (
                        id TEXT PRIMARY KEY NOT NULL,
                        userId TEXT NOT NULL,
                        orderCode TEXT,
                        items TEXT NOT NULL,
                        totalAmount REAL NOT NULL,
                        timestamp INTEGER NOT NULL,
                        status TEXT NOT NULL
                    )
                """)

                // Copy data from old table to new table, converting id to TEXT
                database.execSQL("""
                    INSERT INTO orders (id, userId, orderCode, items, totalAmount, timestamp, status)
                    SELECT CAST(id AS TEXT), userId, orderCode, items, totalAmount, timestamp, status
                    FROM orders_old
                """)

                // Drop the old table
                database.execSQL("DROP TABLE orders_old")

                // Add isAvailable and lastUpdated columns to food_items if they don't exist
                database.execSQL("ALTER TABLE food_items ADD COLUMN isAvailable INTEGER NOT NULL DEFAULT 1")
                database.execSQL("ALTER TABLE food_items ADD COLUMN lastUpdated INTEGER NOT NULL DEFAULT 0")
                
                // Create stock_items table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS stock_items (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        quantity INTEGER NOT NULL,
                        unit TEXT NOT NULL,
                        minimumQuantity INTEGER NOT NULL DEFAULT 0,
                        category TEXT NOT NULL,
                        lastUpdated INTEGER NOT NULL
                    )
                """)
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                try {
                    val instance = Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "app_database"
                    )
                        .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
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
