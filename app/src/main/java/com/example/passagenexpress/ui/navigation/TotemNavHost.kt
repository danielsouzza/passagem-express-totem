package com.example.passagenexpress.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.example.passagenexpress.feature.city.navigation.CITY_ROUTE
import com.example.passagenexpress.feature.city.navigation.cityScreen
import com.example.passagenexpress.feature.date.navigation.dateRoute
import com.example.passagenexpress.feature.date.navigation.dateScreen
import com.example.passagenexpress.feature.idle.navigation.IDLE_ROUTE
import com.example.passagenexpress.feature.idle.navigation.idleScreen
import com.example.passagenexpress.feature.passenger.navigation.passengerRoute
import com.example.passagenexpress.feature.passenger.navigation.passengerScreen
import com.example.passagenexpress.feature.payment.navigation.paymentRoute
import com.example.passagenexpress.feature.payment.navigation.paymentScreen
import com.example.passagenexpress.feature.print.navigation.printRoute
import com.example.passagenexpress.feature.print.navigation.printScreen
import com.example.passagenexpress.feature.room.navigation.roomRoute
import com.example.passagenexpress.feature.room.navigation.roomScreen
import com.example.passagenexpress.feature.setup.navigation.SETUP_ROUTE
import com.example.passagenexpress.feature.setup.navigation.setupScreen
import com.example.passagenexpress.feature.trip.navigation.tripRoute
import com.example.passagenexpress.feature.trip.navigation.tripScreen

@Composable
fun TotemNavHost(
    navController: NavHostController,
    startDestination: String,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        // Avançar = nova tela vem da direita, antiga sai pela esquerda.
        // Voltar  = tela anterior volta pela esquerda, atual sai pela direita.
        enterTransition = { slideIntoContainer(SlideDirection.Left, tween(NAV_TRANSITION_MS)) },
        exitTransition = { slideOutOfContainer(SlideDirection.Left, tween(NAV_TRANSITION_MS)) },
        popEnterTransition = { slideIntoContainer(SlideDirection.Right, tween(NAV_TRANSITION_MS)) },
        popExitTransition = { slideOutOfContainer(SlideDirection.Right, tween(NAV_TRANSITION_MS)) },
    ) {
        setupScreen(
            onSetupCompleted = {
                navController.navigate(IDLE_ROUTE) {
                    popUpTo(SETUP_ROUTE) { inclusive = true }
                    launchSingleTop = true
                }
            },
        )
        idleScreen(
            onStart = {
                navController.navigate(CITY_ROUTE) {
                    launchSingleTop = true
                }
            },
            onSecretLongPress = {
                navController.navigate(SETUP_ROUTE) {
                    popUpTo(IDLE_ROUTE) { inclusive = true }
                    launchSingleTop = true
                }
            },
        )
        cityScreen(
            onDestinoSelected = { destino ->
                navController.navigate(dateRoute(destino.slug, destino.nome)) {
                    launchSingleTop = true
                }
            },
            onBack = {
                navController.popBackStack(IDLE_ROUTE, inclusive = false)
            },
        )
        dateScreen(
            onDateConfirmed = { destinoSlug, destinoNome, date ->
                navController.navigate(tripRoute(destinoSlug, destinoNome, date)) {
                    launchSingleTop = true
                }
            },
            onBack = {
                navController.popBackStack()
            },
        )
        tripScreen(
            onTripConfirmed = { trecho ->
                navController.navigate(roomRoute(trecho)) {
                    launchSingleTop = true
                }
            },
            onBack = {
                navController.popBackStack()
            },
        )
        roomScreen(
            onSaleStarted = { sale, rawTrechoArg ->
                navController.navigate(passengerRoute(sale, rawTrechoArg)) {
                    launchSingleTop = true
                }
            },
            onBack = {
                navController.popBackStack()
            },
        )
        passengerScreen(
            onPassengersCompleted = { completed ->
                navController.navigate(
                    paymentRoute(completed.rawTrechoArg, completed.passageiros, completed.contato)
                ) {
                    launchSingleTop = true
                }
            },
            onBack = {
                navController.popBackStack()
            },
        )
        paymentScreen(
            // Pagamento aprovado: segue pra etapa de impressão dos bilhetes (última do fluxo).
            onPaid = { approved ->
                navController.navigate(printRoute(approved.pedidoId)) {
                    launchSingleTop = true
                }
            },
            onBack = {
                navController.popBackStack()
            },
        )
        printScreen(
            // Bilhetes impressos (ou cliente concluiu): limpa a pilha da compra e volta pro Idle.
            onFinished = {
                navController.navigate(IDLE_ROUTE) {
                    popUpTo(IDLE_ROUTE) { inclusive = false }
                    launchSingleTop = true
                }
            },
        )
    }
}

private const val NAV_TRANSITION_MS = 280

