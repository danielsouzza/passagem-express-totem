package com.example.passagenexpress.feature.idle.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.passagenexpress.feature.idle.IdleScreen

const val IDLE_ROUTE = "idle"

fun NavGraphBuilder.idleScreen(
    onStart: () -> Unit,
    onSecretLongPress: () -> Unit,
    onSettings: () -> Unit,
) {
    composable(IDLE_ROUTE) {
        IdleScreen(
            onStart = onStart,
            onSecretLongPress = onSecretLongPress,
            onSettings = onSettings,
        )
    }
}
