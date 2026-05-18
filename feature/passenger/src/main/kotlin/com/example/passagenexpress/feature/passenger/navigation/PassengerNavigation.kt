package com.example.passagenexpress.feature.passenger.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.passagenexpress.core.domain.repository.InicioVenda
import com.example.passagenexpress.feature.passenger.PassengerCompleted
import com.example.passagenexpress.feature.passenger.PassengerScreen
import com.example.passagenexpress.feature.passenger.PassengerViewModel
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

const val PASSENGER_ROUTE =
    "passenger/{${PassengerViewModel.ARG_TRECHO}}/{${PassengerViewModel.ARG_INICIO_VENDA}}"

/**
 * `decodedTrechoJson` é o JSON cru (Navigation/SavedStateHandle já desfaz o URL-encode quando o
 * arg entra no ViewModel anterior); precisa ser re-encodado aqui antes de virar segmento de rota.
 */
fun passengerRoute(inicioVenda: InicioVenda, decodedTrechoJson: String): String {
    val trechoSegment = URLEncoder.encode(decodedTrechoJson, StandardCharsets.UTF_8.name())
    return "passenger/$trechoSegment/${encodeInicioVendaArg(inicioVenda)}"
}

fun NavGraphBuilder.passengerScreen(
    onPassengersCompleted: (PassengerCompleted) -> Unit,
    onBack: () -> Unit,
) {
    composable(
        route = PASSENGER_ROUTE,
        arguments = listOf(
            navArgument(PassengerViewModel.ARG_TRECHO) { type = NavType.StringType },
            navArgument(PassengerViewModel.ARG_INICIO_VENDA) { type = NavType.StringType },
        ),
    ) {
        PassengerScreen(onPassengersCompleted = onPassengersCompleted, onBack = onBack)
    }
}
