package com.example.passagenexpress.feature.settings.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.passagenexpress.feature.settings.SettingsScreen

const val SETTINGS_ROUTE = "settings"

fun NavGraphBuilder.settingsScreen(onBack: () -> Unit) {
    composable(SETTINGS_ROUTE) {
        SettingsScreen(onBack = onBack)
    }
}
