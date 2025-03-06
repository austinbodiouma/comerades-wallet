package com.example.commeradeswallet.data.converter

import androidx.room.TypeConverter
import com.example.commeradeswallet.data.model.CartItem
import com.example.commeradeswallet.data.model.OrderStatus
import com.example.commeradeswallet.data.model.TransactionType
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.LocalDateTime
import java.time.ZoneOffset
import com.example.commeradeswallet.data.model.TransactionStatus

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromTimestamp(value: Long?): LocalDateTime? {
        return value?.let { LocalDateTime.ofEpochSecond(it, 0, ZoneOffset.UTC) }
    }

    @TypeConverter
    fun dateToTimestamp(date: LocalDateTime?): Long? {
        return date?.toEpochSecond(ZoneOffset.UTC)
    }

    @TypeConverter
    fun fromCartItemList(value: List<CartItem>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toCartItemList(value: String): List<CartItem> {
        val listType = object : TypeToken<List<CartItem>>() {}.type
        return gson.fromJson(value, listType)
    }

    @TypeConverter
    fun fromOrderStatus(value: OrderStatus): String {
        return value.name
    }

    @TypeConverter
    fun toOrderStatus(value: String): OrderStatus {
        return OrderStatus.valueOf(value)
    }

    @TypeConverter
    fun fromTransactionType(value: TransactionType): String {
        return value.name
    }

    @TypeConverter
    fun toTransactionType(value: String): TransactionType {
        return TransactionType.valueOf(value)
    }

    @TypeConverter
    fun fromTransactionStatus(value: TransactionStatus): String {
        return value.name
    }

    @TypeConverter
    fun toTransactionStatus(value: String): TransactionStatus {
        return TransactionStatus.valueOf(value)
    }
} 