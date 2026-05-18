package com.example.passagenexpress.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.example.passagenexpress.core.designsystem.component.TotemLoading
import com.example.passagenexpress.feature.idle.navigation.IDLE_ROUTE
import com.example.passagenexpress.feature.setup.navigation.SETUP_ROUTE
import com.example.passagenexpress.ui.chrome.TotemAppChrome
import com.example.passagenexpress.ui.navigation.TotemNavHost

@Composable
fun TotemApp(viewModel: TotemRootViewModel = hiltViewModel()) {
    val destination by viewModel.rootDestination.collectAsStateWithLifecycle()
    val navController = rememberNavController()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        when (destination) {
            RootDestination.Loading -> TotemLoading()
            RootDestination.Setup -> TotemNavHost(
                navController = navController,
                startDestination = SETUP_ROUTE,
            )
            RootDestination.Idle -> TotemAppChrome {
                TotemNavHost(
                    navController = navController,
                    startDestination = IDLE_ROUTE,
                )
            }
        }
    }
}
