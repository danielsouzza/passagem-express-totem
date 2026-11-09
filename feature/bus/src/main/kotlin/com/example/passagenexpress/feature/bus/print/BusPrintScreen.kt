package com.example.passagenexpress.feature.bus.print

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.passagenexpress.core.designsystem.component.TotemLoading
import com.example.passagenexpress.core.designsystem.component.TotemPrimaryButton
import com.example.passagenexpress.core.designsystem.component.TotemScreenScaffold
import com.example.passagenexpress.core.designsystem.component.TotemSecondaryButton
import com.example.passagenexpress.core.designsystem.theme.TotemPalette
import com.example.passagenexpress.core.designsystem.theme.TotemTheme
import com.example.passagenexpress.feature.bus.R
import kotlinx.coroutines.delay

@Composable
fun BusPrintScreen(
    onFinished: () -> Unit,
    viewModel: BusPrintViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    TotemScreenScaffold(title = stringResource(R.string.bus_print_title), showCancel = false) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            when (state) {
                BusPrintState.Emitindo -> TotemLoading(label = stringResource(R.string.bus_print_issuing))
                BusPrintState.Imprimindo -> TotemLoading(label = stringResource(R.string.bus_print_printing))
                BusPrintState.Concluido -> Concluido(onFinished)
                BusPrintState.Falhou -> Falhou(onRetry = viewModel::onRetry, onFinish = onFinished)
            }
        }
    }
}

@Composable
private fun Concluido(onFinished: () -> Unit) {
    // Volta sozinho pro início depois de alguns segundos (próximo cliente).
    var restante by remember { mutableIntStateOf(AUTO_FINISH_SECONDS) }
    LaunchedEffect(Unit) {
        while (restante > 0) {
            delay(1_000)
            restante--
        }
        onFinished()
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space16),
        modifier = Modifier.widthIn(max = 640.dp).padding(24.dp),
    ) {
        Box(
            modifier = Modifier.size(120.dp).background(TotemPalette.SuccessLight, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.CheckCircle, null, tint = TotemPalette.Success, modifier = Modifier.size(72.dp))
        }
        Text(
            text = stringResource(R.string.bus_print_done_title),
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = TotemPalette.Ink,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.bus_print_done_message),
            style = MaterialTheme.typography.bodyLarge,
            color = TotemPalette.InkMuted,
            textAlign = TextAlign.Center,
        )
        TotemPrimaryButton(
            text = stringResource(R.string.bus_print_finish, restante),
            onClick = onFinished,
            trailingArrow = false,
        )
    }
}

@Composable
private fun Falhou(onRetry: () -> Unit, onFinish: () -> Unit) {
    // Mesma UX amigável do fluxo de barco: sem detalhe técnico pro cliente.
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier.widthIn(max = 640.dp).padding(24.dp),
    ) {
        Text(
            text = stringResource(R.string.bus_print_failed_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.bus_print_failed_hint),
            style = MaterialTheme.typography.bodyLarge,
            color = TotemPalette.Ink,
            textAlign = TextAlign.Center,
        )
        TotemPrimaryButton(text = stringResource(R.string.bus_retry), onClick = onRetry, trailingArrow = false)
        TotemSecondaryButton(text = stringResource(R.string.bus_finish), onClick = onFinish)
    }
}

private const val AUTO_FINISH_SECONDS = 20
