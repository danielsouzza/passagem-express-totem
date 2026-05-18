package com.example.passagenexpress.feature.payment.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.passagenexpress.feature.passenger.ContatoForm
import com.example.passagenexpress.feature.passenger.PassageiroForm
import com.example.passagenexpress.feature.payment.PaymentApproved
import com.example.passagenexpress.feature.payment.PaymentScreen
import com.example.passagenexpress.feature.payment.PaymentViewModel
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

const val PAYMENT_ROUTE =
    "payment/{${PaymentViewModel.ARG_TRECHO}}/{${PaymentViewModel.ARG_SALE}}"

/**
 * `decodedTrechoJson` é o JSON cru (Navigation/SavedStateHandle decoda o arg ao entregar pro
 * ViewModel anterior); precisa ser re-encodado aqui antes de virar segmento de rota.
 */
fun paymentRoute(
    decodedTrechoJson: String,
    passageiros: List<PassageiroForm>,
    contato: ContatoForm,
): String {
    val trechoSegment = URLEncoder.encode(decodedTrechoJson, StandardCharsets.UTF_8.name())
    return "payment/$trechoSegment/${buildSaleArg(passageiros, contato)}"
}

fun NavGraphBuilder.paymentScreen(
    onPaid: (PaymentApproved) -> Unit,
    onBack: () -> Unit,
) {
    composable(
        route = PAYMENT_ROUTE,
        arguments = listOf(
            navArgument(PaymentViewModel.ARG_TRECHO) { type = NavType.StringType },
            navArgument(PaymentViewModel.ARG_SALE) { type = NavType.StringType },
        ),
    ) {
        PaymentScreen(onPaid = onPaid, onBack = onBack)
    }
}
