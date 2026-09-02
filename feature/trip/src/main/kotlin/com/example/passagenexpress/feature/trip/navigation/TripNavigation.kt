package com.example.passagenexpress.feature.trip.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.passagenexpress.core.domain.model.Trecho
import com.example.passagenexpress.feature.trip.TripScreen

// Rota sem argumentos: a tela abre direto da Idle e busca as próximas viagens (irrestrito).
// Destino e data são filtros opcionais aplicados na própria tela.
const val TRIP_ROUTE = "trip"

fun NavGraphBuilder.tripScreen(
    onTripConfirmed: (Trecho) -> Unit,
    onBack: () -> Unit,
) {
    composable(TRIP_ROUTE) {
        TripScreen(onTripConfirmed = onTripConfirmed, onBack = onBack)
    }
}
