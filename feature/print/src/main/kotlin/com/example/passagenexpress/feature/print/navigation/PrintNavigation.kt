package com.example.passagenexpress.feature.print.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.passagenexpress.feature.print.PrintScreen
import com.example.passagenexpress.feature.print.PrintViewModel

const val PRINT_ROUTE = "print/{${PrintViewModel.ARG_PEDIDO_ID}}"

fun printRoute(pedidoId: Long): String = "print/$pedidoId"

fun NavGraphBuilder.printScreen(onFinished: () -> Unit) {
    composable(
        route = PRINT_ROUTE,
        arguments = listOf(
            navArgument(PrintViewModel.ARG_PEDIDO_ID) { type = NavType.LongType },
        ),
    ) {
        PrintScreen(onFinished = onFinished)
    }
}
