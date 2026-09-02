package com.example.passagenexpress.feature.print

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.passagenexpress.core.designsystem.component.TotemLoading
import com.example.passagenexpress.core.designsystem.component.TotemPrimaryButton
import com.example.passagenexpress.core.designsystem.component.TotemScreenScaffold
import com.example.passagenexpress.core.designsystem.component.TotemSecondaryButton
import com.example.passagenexpress.core.designsystem.theme.TotemPalette
import com.example.passagenexpress.feature.print.R
import kotlinx.coroutines.delay

private const val AUTO_RETURN_SECONDS = 10

@Composable
fun PrintScreen(
    onFinished: () -> Unit,
    viewModel: PrintViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    TotemScreenScaffold(title = stringResource(R.string.print_title)) {
        when (val s = state) {
            is PrintUiState.Preparing -> TotemLoading(label = stringResource(R.string.print_preparing))

            is PrintUiState.Printing -> TotemLoading(
                label = stringResource(R.string.print_progress, s.current, s.total),
            )

            is PrintUiState.Done -> DoneContent(onFinished = onFinished)

            is PrintUiState.Failed -> FailedContent(
                message = s.message,
                onRetry = viewModel::onRetry,
                onFinish = onFinished,
            )
        }
    }
}

@Composable
private fun DoneContent(onFinished: () -> Unit) {
    // Só agora (bilhete impresso) entra o countdown + botão de continuar comprando.
    var seconds by remember { mutableIntStateOf(AUTO_RETURN_SECONDS) }
    LaunchedEffect(Unit) {
        while (seconds > 0) {
            delay(1_000)
            seconds -= 1
        }
        onFinished()
    }
    CenteredColumn {
        Text(
            text = stringResource(R.string.print_done_title),
            style = MaterialTheme.typography.headlineMedium,
            color = TotemPalette.Ink,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.print_done_message),
            style = MaterialTheme.typography.bodyLarge,
            color = TotemPalette.InkMuted,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.print_auto_return, seconds),
            style = MaterialTheme.typography.titleMedium,
            color = TotemPalette.InkMuted,
            textAlign = TextAlign.Center,
        )
        TotemPrimaryButton(
            text = stringResource(R.string.print_continue),
            onClick = onFinished,
            trailingArrow = false,
        )
    }
}

@Composable
private fun FailedContent(
    message: String,
    onRetry: () -> Unit,
    onFinish: () -> Unit,
) {
    CenteredColumn {
        Text(
            text = stringResource(R.string.print_failed_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = TotemPalette.InkMuted,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.print_failed_hint),
            style = MaterialTheme.typography.bodyLarge,
            color = TotemPalette.Ink,
            textAlign = TextAlign.Center,
        )
        TotemPrimaryButton(text = stringResource(R.string.print_retry), onClick = onRetry, trailingArrow = false)
        TotemSecondaryButton(text = stringResource(R.string.print_finish), onClick = onFinish)
    }
}

@Composable
private fun CenteredColumn(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) { content() }
    }
}
