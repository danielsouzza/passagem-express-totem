package com.example.passagenexpress.feature.room.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.passagenexpress.core.domain.model.Trecho
import com.example.passagenexpress.core.domain.repository.InicioVenda
import com.example.passagenexpress.feature.room.RoomScreen
import com.example.passagenexpress.feature.room.RoomViewModel

const val ROOM_ROUTE = "room/{${RoomViewModel.ARG_TRECHO}}"

fun roomRoute(trecho: Trecho): String = "room/${encodeTrechoArg(trecho)}"

fun NavGraphBuilder.roomScreen(
    onSaleStarted: (InicioVenda, String) -> Unit,
    onBack: () -> Unit,
) {
    composable(
        route = ROOM_ROUTE,
        arguments = listOf(
            navArgument(RoomViewModel.ARG_TRECHO) { type = NavType.StringType },
        ),
    ) {
        RoomScreen(onSaleStarted = onSaleStarted, onBack = onBack)
    }
}
