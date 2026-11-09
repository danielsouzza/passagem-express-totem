package com.example.passagenexpress.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.example.passagenexpress.core.domain.model.Modalidade
import com.example.passagenexpress.feature.bus.navigation.BUS_GRAPH_ROUTE
import com.example.passagenexpress.feature.bus.navigation.MODAL_CHOICE_ROUTE
import com.example.passagenexpress.feature.bus.navigation.busGraph
import com.example.passagenexpress.feature.bus.navigation.modalChoiceScreen
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
import com.example.passagenexpress.feature.settings.navigation.SETTINGS_ROUTE
import com.example.passagenexpress.feature.settings.navigation.settingsScreen
import com.example.passagenexpress.feature.setup.navigation.SETUP_ROUTE
import com.example.passagenexpress.feature.setup.navigation.setupScreen
import com.example.passagenexpress.feature.trip.navigation.TRIP_ROUTE
import com.example.passagenexpress.feature.trip.navigation.tripScreen

@Composable
fun TotemNavHost(
    navController: NavHostController,
    startDestination: String,
    modifier: Modifier = Modifier,
    /** O que o totem vende — decide pra onde o "Começar" leva. */
    modalidades: Set<Modalidade> = setOf(Modalidade.Hidroviario),
) {
    // Volta ao Idle limpando a pilha da compra (mesmo efeito do botão Início).
    val voltarAoIdle: () -> Unit = {
        navController.navigate(IDLE_ROUTE) {
            popUpTo(IDLE_ROUTE) { inclusive = false }
            launchSingleTop = true
        }
    }
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
                // Só barco → fluxo hidroviário original (inalterado); só ônibus → fluxo rodoviário;
                // as duas → pergunta ao cliente.
                val destino = when {
                    Modalidade.Hidroviario in modalidades && Modalidade.Rodoviario in modalidades -> MODAL_CHOICE_ROUTE
                    Modalidade.Rodoviario in modalidades -> BUS_GRAPH_ROUTE
                    else -> TRIP_ROUTE
                }
                navController.navigate(destino) {
                    launchSingleTop = true
                }
            },
            onSecretLongPress = {
                navController.navigate(SETUP_ROUTE) {
                    popUpTo(IDLE_ROUTE) { inclusive = true }
                    launchSingleTop = true
                }
            },
            onSettings = {
                navController.navigate(SETTINGS_ROUTE) {
                    launchSingleTop = true
                }
            },
        )
        modalChoiceScreen(
            onBoat = { navController.navigate(TRIP_ROUTE) { launchSingleTop = true } },
            onBus = { navController.navigate(BUS_GRAPH_ROUTE) { launchSingleTop = true } },
            onBack = { navController.popBackStack() },
        )
        busGraph(
            navController = navController,
            onExit = { navController.popBackStack() },
            onFinished = voltarAoIdle,
        )
        settingsScreen(
            onBack = {
                navController.popBackStack(IDLE_ROUTE, inclusive = false)
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

private const val NAV_TRANSITION_MS = 220

