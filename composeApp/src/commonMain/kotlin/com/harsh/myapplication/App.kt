package com.harsh.myapplication

import androidx.compose.animation.*
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.unit.IntOffset
import com.harsh.myapplication.ui.components.BackHandler
import com.harsh.myapplication.ui.screens.AddMemoryScreen
import com.harsh.myapplication.ui.screens.MemoryQueryScreen
import com.harsh.myapplication.ui.screens.PersonDetailScreen
import com.harsh.myapplication.ui.screens.PersonListScreen
import org.koin.compose.KoinContext

@Composable
fun App() {
    // Navigation state
    val backStack = remember { mutableStateListOf("list") }
    val currentScreen = backStack.lastOrNull() ?: "list"
    var selectedPersonId by remember { mutableStateOf<String?>(null) }

    // Track navigation direction for animation
    var isBackNavigation by remember { mutableStateOf(false) }

    // Helper to navigate forward
    fun navigateTo(screen: String) {
        if (currentScreen != screen) {
            isBackNavigation = false // Going Forward
            backStack.add(screen)
        }
    }

    // Helper to go back
    fun goBack() {
        if (backStack.size > 1) {
            isBackNavigation = true // Going Back
            backStack.removeAt(backStack.lastIndex)
        }
    }

    // Handles Android Hardware Back Button
    BackHandler(enabled = backStack.size > 1) {
        goBack()
    }

    KoinContext {
        MaterialTheme {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    val spec = tween<IntOffset>(350, easing = LinearOutSlowInEasing)
                    if (isBackNavigation) {
                        slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, spec) togetherWith
                                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, spec)
                    } else {
                        slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, spec) togetherWith
                                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, spec)
                    }
                }
            ) { screen ->
                when (screen) {
                    "list" -> PersonListScreen(
                        onNavigateToAddMemory = {
                            selectedPersonId = null
                            navigateTo("add_memory")
                        },
                        onNavigateToPersonDetail = { personId ->
                            selectedPersonId = personId
                            navigateTo("person_detail")
                        },
                        onNavigateToChat = { navigateTo("chat") }
                    )

                    "chat" -> MemoryQueryScreen(
                        onBackClick = { goBack() }
                    )

                    "person_detail" -> {
                        selectedPersonId?.let { personId ->
                            PersonDetailScreen(
                                personId = personId,
                                onNavigateBack = { goBack() },
                                onNavigateToAddMemory = { navigateTo("add_memory") }
                            )
                        }
                    }

                    "add_memory" -> AddMemoryScreen(
                        preSelectedPersonId = selectedPersonId,
                        onNavigateBack = {
                            goBack()
                        }
                    )
                }
            }
        }
    }
}