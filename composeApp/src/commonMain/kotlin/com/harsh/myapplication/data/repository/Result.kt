package com.harsh.myapplication.data.repository

/**
 * A sealed class representing the result of an operation
 * Used to handle success and failure cases cleanly
 */
sealed class Result<out T> {
    /**
     * Success case - contains the data
     */
    data class Success<T>(val data: T) : Result<T>()

    /**
     * Error case - contains error message
     */
    data class Error(val message: String, val exception: Exception? = null) : Result<Nothing>()

    /**
     * Loading state (optional - can be used for UI)
     */
    object Loading : Result<Nothing>()
}

/**
 * Extension function to check if result is success
 */
fun <T> Result<T>.isSuccess(): Boolean = this is Result.Success

/**
 * Extension function to check if result is error
 */
fun <T> Result<T>.isError(): Boolean = this is Result.Error

/**
 * Extension function to get data or null
 */
fun <T> Result<T>.getOrNull(): T? {
    return when (this) {
        is Result.Success -> data
        else -> null
    }
}

/**
 * Extension function to get data or default value
 */
fun <T> Result<T>.getOrDefault(default: T): T {
    return when (this) {
        is Result.Success -> data
        else -> default
    }
}
