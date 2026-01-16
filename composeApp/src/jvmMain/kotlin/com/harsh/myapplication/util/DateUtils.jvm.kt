package com.harsh.myapplication.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

actual fun formatDate(timestamp: Long): String {
    val date = Date(timestamp)
    val sdf = SimpleDateFormat("MMM dd, yyyy • h:mm a", Locale.getDefault())
    return sdf.format(date)
}

actual fun getCurrentTimeMillis(): Long {
    return System.currentTimeMillis()
}