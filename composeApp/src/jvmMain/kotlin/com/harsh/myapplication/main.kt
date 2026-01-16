package com.harsh.myapplication

import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.harsh.myapplication.di.initKoin

fun main() {
    // Initialize Koin BEFORE creating window
    initKoin()

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Memory Companion",
            icon = painterResource("icon.png")
        ) {
            App()
        }
    }
}