package com.example.passagenexpress.feature.date.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.passagenexpress.feature.date.DateScreen
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.LocalDate

private const val ARG_DESTINO_SLUG = "destinoSlug"
private const val ARG_DESTINO_NOME = "destinoNome"

const val DATE_ROUTE = "date/{$ARG_DESTINO_SLUG}/{$ARG_DESTINO_NOME}"

fun dateRoute(destinoSlug: String, destinoNome: String): String {
    val d = URLEncoder.encode(destinoSlug, StandardCharsets.UTF_8.name())
    val dn = URLEncoder.encode(destinoNome, StandardCharsets.UTF_8.name())
    return "date/$d/$dn"
}

fun NavGraphBuilder.dateScreen(
    onDateConfirmed: (destinoSlug: String, destinoNome: String, date: LocalDate) -> Unit,
    onBack: () -> Unit,
) {
    composable(
        route = DATE_ROUTE,
        arguments = listOf(
            navArgument(ARG_DESTINO_SLUG) { type = NavType.StringType },
            navArgument(ARG_DESTINO_NOME) { type = NavType.StringType },
        ),
    ) { entry ->
        val d = entry.arguments?.getString(ARG_DESTINO_SLUG).orEmpty()
        val dn = entry.arguments?.getString(ARG_DESTINO_NOME).orEmpty()
        DateScreen(
            onDateConfirmed = { date -> onDateConfirmed(d, dn, date) },
            onBack = onBack,
        )
    }
}
