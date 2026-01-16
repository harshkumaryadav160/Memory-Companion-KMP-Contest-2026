package com.harsh.myapplication.ui.components
import androidx.compose.runtime.Composable
import androidx.activity.compose.BackHandler

@Composable
actual fun BackHandler(enabled: Boolean, onBack: () -> Unit) {
    // Delegates to the real Android BackHandler
    BackHandler(enabled, onBack)
}