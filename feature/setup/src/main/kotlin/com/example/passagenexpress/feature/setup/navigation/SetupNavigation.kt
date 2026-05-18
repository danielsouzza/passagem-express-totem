package com.example.passagenexpress.feature.setup.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.passagenexpress.feature.setup.SetupWizardScreen

const val SETUP_ROUTE = "setup"

fun NavGraphBuilder.setupScreen(onSetupCompleted: () -> Unit) {
    composable(SETUP_ROUTE) {
        SetupWizardScreen(onSetupCompleted = onSetupCompleted)
    }
}
