package com.example.passagenexpress.feature.city.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.passagenexpress.core.domain.model.Municipio
import com.example.passagenexpress.feature.city.CityScreen

const val CITY_ROUTE = "city"

fun NavGraphBuilder.cityScreen(
    onDestinoSelected: (Municipio) -> Unit,
    onBack: () -> Unit,
) {
    composable(CITY_ROUTE) {
        CityScreen(onDestinoSelected = onDestinoSelected, onBack = onBack)
    }
}
