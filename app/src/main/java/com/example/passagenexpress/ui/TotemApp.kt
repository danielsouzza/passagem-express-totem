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
import com.example.passagenexpress.ui.locale.ProvideAppLocale
import com.example.passagenexpress.ui.navigation.TotemNavHost

@Composable
fun TotemApp(viewModel: TotemRootViewModel = hiltViewModel()) {
    val destination by viewModel.rootDestination.collectAsStateWithLifecycle()
    val language by viewModel.language.collectAsStateWithLifecycle()
    val navController = rememberNavController()

    ProvideAppLocale(language = language) {
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
                RootDestination.Idle -> {
                    // Volta ao Idle limpando a pilha da compra. Usado pelo botão Início (topo) e
                    // pelo Cancelar sutil no rodapé de toda flow screen.
                    val resetToIdle: () -> Unit = {
                        navController.navigate(IDLE_ROUTE) {
                            popUpTo(IDLE_ROUTE) { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                    TotemAppChrome(
                        onHomeClick = resetToIdle,
                        onCancel = resetToIdle,
                    ) {
                        TotemNavHost(
                            navController = navController,
                            startDestination = IDLE_ROUTE,
                        )
                    }
                }
            }
        }
    }
}
