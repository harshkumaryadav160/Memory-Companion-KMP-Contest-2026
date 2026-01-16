package com.harsh.myapplication.ui.components
import androidx.compose.runtime.Composable

@Composable
actual fun BackHandler(enabled: Boolean, onBack: () -> Unit) {
    // Desktop doesn't have a hardware back button, so we do nothing
}