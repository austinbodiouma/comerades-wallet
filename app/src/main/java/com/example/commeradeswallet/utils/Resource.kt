package com.example.commeradeswallet.utils

/**
 * A generic class that holds a value with its loading status.
 * @param <T>
 */
sealed class Resource<out T> {
    data class Success<out T>(val data: T?) : Resource<T>()
    data class Error(val message: String?, val exception: Exception? = null) : Resource<Nothing>()
    object Loading : Resource<Nothing>()

    override fun toString(): String {
        return when (this) {
            is Success<*> -> "Success[data=$data]"
            is Error -> "Error[message=$message, exception=$exception]"
            is Loading -> "Loading"
        }
    }
} 