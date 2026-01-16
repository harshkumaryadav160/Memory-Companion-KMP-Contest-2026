package com.harsh.myapplication.util

// Defines a function that every platform MUST implement
expect fun formatDate(timestamp: Long): String

expect fun getCurrentTimeMillis(): Long