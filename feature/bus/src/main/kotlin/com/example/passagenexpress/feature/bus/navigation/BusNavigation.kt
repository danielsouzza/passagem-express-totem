package com.example.passagenexpress.feature.bus.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import com.example.passagenexpress.feature.bus.BusSaleSession
import com.example.passagenexpress.feature.bus.Sentido
import com.example.passagenexpress.feature.bus.choice.ModalChoiceScreen
import com.example.passagenexpress.feature.bus.passengers.BusPassengersScreen
import com.example.passagenexpress.feature.bus.payment.BusPaymentScreen
import com.example.passagenexpress.feature.bus.print.BusPrintScreen
import com.example.passagenexpress.feature.bus.print.BusPrintViewModel
import com.example.passagenexpress.feature.bus.search.BusSearchScreen
import com.example.passagenexpress.feature.bus.seats.BusSeatsScreen
import com.example.passagenexpress.feature.bus.seats.BusSeatsViewModel
import com.example.passagenexpress.feature.bus.trips.BusTripsScreen
import com.example.passagenexpress.feature.bus.trips.BusTripsViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/** Tela "barco ou ônibus?" (só quando o totem vende as duas modalidades). */
const val MODAL_CHOICE_ROUTE = "modal_choice"

/** Grafo aninhado do fluxo de ônibus — entrar nele = nova venda; sair dele = sessão encerrada. */
const val BUS_GRAPH_ROUTE = "bus"

private const val SEARCH_ROUTE = "bus/search"
private const val TRIPS_ROUTE = "bus/trips/{${BusTripsViewModel.ARG_SENTIDO}}"
private const val SEATS_ROUTE = "bus/seats/{${BusSeatsViewModel.ARG_SENTIDO}}"
private const val PASSENGERS_ROUTE = "bus/passengers"
private const val PAYMENT_ROUTE = "bus/payment"
private const val PRINT_ROUTE = "bus/print/{${BusPrintViewModel.ARG_PEDIDO_ID}}"

private fun tripsRoute(sentido: Sentido) = "bus/trips/${sentido.name}"
private fun seatsRoute(sentido: Sentido) = "bus/seats/${sentido.name}"
private fun printRoute(pedidoId: Long) = "bus/print/$pedidoId"

/**
 * Dono do ciclo de vida da venda: vive no back stack entry do grafo `bus`. Quando o grafo sai da
 * pilha (Início, Cancelar, fim da compra), [onCleared] encerra a sessão — liberando os assentos
 * bloqueados se a venda não foi paga.
 */
@HiltViewModel
class BusFlowViewModel @Inject constructor(
    val session: BusSaleSession,
) : ViewModel() {
    override fun onCleared() {
        session.encerrar()
        super.onCleared()
    }
}

@Composable
private fun flowViewModel(navController: NavHostController, entry: NavBackStackEntry): BusFlowViewModel {
    val parent = remember(entry) { navController.getBackStackEntry(BUS_GRAPH_ROUTE) }
    return hiltViewModel(parent)
}

fun NavGraphBuilder.modalChoiceScreen(
    onBoat: () -> Unit,
    onBus: () -> Unit,
    onBack: () -> Unit,
) {
    composable(MODAL_CHOICE_ROUTE) {
        ModalChoiceScreen(onBoat = onBoat, onBus = onBus, onBack = onBack)
    }
}

/**
 * @param onExit voltar a partir da busca (sai do fluxo de ônibus).
 * @param onFinished fim da compra (impressão concluída) — volta pro Idle.
 */
fun NavGraphBuilder.busGraph(
    navController: NavHostController,
    onExit: () -> Unit,
    onFinished: () -> Unit,
) {
    navigation(startDestination = SEARCH_ROUTE, route = BUS_GRAPH_ROUTE) {
        composable(SEARCH_ROUTE) { entry ->
            flowViewModel(navController, entry)
            BusSearchScreen(
                onSearch = { navController.navigate(tripsRoute(Sentido.Ida)) { launchSingleTop = true } },
                onBack = onExit,
            )
        }
        composable(
            route = TRIPS_ROUTE,
            arguments = listOf(navArgument(BusTripsViewModel.ARG_SENTIDO) { type = NavType.StringType }),
        ) { entry ->
            flowViewModel(navController, entry)
            val sentido = entry.sentido()
            BusTripsScreen(
                onTripSelected = { navController.navigate(seatsRoute(sentido)) { launchSingleTop = true } },
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = SEATS_ROUTE,
            arguments = listOf(navArgument(BusSeatsViewModel.ARG_SENTIDO) { type = NavType.StringType }),
        ) { entry ->
            val flow = flowViewModel(navController, entry)
            val sentido = entry.sentido()
            BusSeatsScreen(
                onDone = {
                    // Ida concluída + ida-e-volta → escolhe a viagem de volta; senão, passageiros.
                    val proxima = if (sentido == Sentido.Ida && flow.session.value.idaEVolta) {
                        tripsRoute(Sentido.Volta)
                    } else {
                        PASSENGERS_ROUTE
                    }
                    navController.navigate(proxima) { launchSingleTop = true }
                },
                onBack = { navController.popBackStack() },
            )
        }
        composable(PASSENGERS_ROUTE) { entry ->
            flowViewModel(navController, entry)
            BusPassengersScreen(
                onDone = { navController.navigate(PAYMENT_ROUTE) { launchSingleTop = true } },
                onBack = { navController.popBackStack() },
            )
        }
        composable(PAYMENT_ROUTE) { entry ->
            flowViewModel(navController, entry)
            BusPaymentScreen(
                onPaid = { pedidoId ->
                    // Pago: tira as telas de compra da pilha (não dá pra voltar pro pagamento).
                    navController.navigate(printRoute(pedidoId)) {
                        popUpTo(SEARCH_ROUTE) { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onSeatsLost = {
                    // Assento perdido na revalidação: recomeça pela escolha da viagem de ida
                    // (as telas de assento saem da pilha e liberam o que ainda estava bloqueado).
                    navController.navigate(tripsRoute(Sentido.Ida)) {
                        popUpTo(SEARCH_ROUTE) { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = PRINT_ROUTE,
            arguments = listOf(navArgument(BusPrintViewModel.ARG_PEDIDO_ID) { type = NavType.LongType }),
        ) { entry ->
            flowViewModel(navController, entry)
            BusPrintScreen(onFinished = onFinished)
        }
    }
}

private fun NavBackStackEntry.sentido(): Sentido =
    arguments?.getString(BusSeatsViewModel.ARG_SENTIDO)
        ?.let { runCatching { Sentido.valueOf(it) }.getOrNull() } ?: Sentido.Ida
