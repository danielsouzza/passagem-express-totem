package com.example.passagenexpress.ui.chrome

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.passagenexpress.R
import com.example.passagenexpress.core.designsystem.component.LocalTotemStatusBar
import com.example.passagenexpress.core.designsystem.component.TotemLanguageOption
import com.example.passagenexpress.core.designsystem.component.TotemStatusBarState
import com.example.passagenexpress.core.domain.model.AppLanguage
import kotlinx.coroutines.delay
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val TimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

@Composable
fun TotemAppChrome(
    viewModel: TotemAppChromeViewModel = hiltViewModel(),
    onHomeClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val brand = stringResource(R.string.app_name)
    val time = rememberClock()

    val statusBar = TotemStatusBarState(
        brand = brand,
        portoNome = state.portoNome,
        time = time,
        language = state.language.toOption(),
        onLanguageChange = { option -> viewModel.onLanguageChange(option.toDomain()) },
        onHomeClick = onHomeClick,
    )

    CompositionLocalProvider(LocalTotemStatusBar provides statusBar) {
        content()
    }
}

@Composable
private fun rememberClock(): String {
    var now by remember { mutableStateOf(LocalTime.now().format(TimeFormatter)) }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalTime.now().format(TimeFormatter)
            val secondsToNextMinute = 60L - LocalTime.now().second
            delay(secondsToNextMinute * 1_000L)
        }
    }
    return now
}

private fun AppLanguage.toOption(): TotemLanguageOption = when (this) {
    AppLanguage.PtBr -> TotemLanguageOption.Pt
    AppLanguage.EnUs -> TotemLanguageOption.En
}

private fun TotemLanguageOption.toDomain(): AppLanguage = when (this) {
    TotemLanguageOption.Pt -> AppLanguage.PtBr
    TotemLanguageOption.En -> AppLanguage.EnUs
}
