package com.example.passagenexpress.feature.trip.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.passagenexpress.core.domain.model.Trecho
import com.example.passagenexpress.feature.trip.TripScreen
import com.example.passagenexpress.feature.trip.TripViewModel
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.LocalDate

const val TRIP_ROUTE =
    "trip/{${TripViewModel.ARG_DESTINO_SLUG}}/{${TripViewModel.ARG_DESTINO_NOME}}/{${TripViewModel.ARG_DATE}}"

fun tripRoute(destinoSlug: String, destinoNome: String, date: LocalDate): String {
    val d = URLEncoder.encode(destinoSlug, StandardCharsets.UTF_8.name())
    val dn = URLEncoder.encode(destinoNome, StandardCharsets.UTF_8.name())
    return "trip/$d/$dn/$date"
}

fun NavGraphBuilder.tripScreen(
    onTripConfirmed: (Trecho) -> Unit,
    onBack: () -> Unit,
) {
    composable(
        route = TRIP_ROUTE,
        arguments = listOf(
            navArgument(TripViewModel.ARG_DESTINO_SLUG) { type = NavType.StringType },
            navArgument(TripViewModel.ARG_DESTINO_NOME) { type = NavType.StringType },
            navArgument(TripViewModel.ARG_DATE) { type = NavType.StringType },
        ),
    ) {
        TripScreen(onTripConfirmed = onTripConfirmed, onBack = onBack)
    }
}
