package com.example.passagenexpress.ui.locale

import android.content.res.Configuration
import android.view.ContextThemeWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.example.passagenexpress.core.domain.model.AppLanguage
import java.util.Locale

/**
 * Aplica o idioma escolhido (PT-BR / EN-US) em tempo de execução, sem recriar a Activity.
 *
 * `stringResource` resolve as strings a partir de `LocalContext.current.resources`, então
 * para trocar o idioma na hora precisamos prover um Context cujos resources usem o locale
 * selecionado. Usamos `ContextThemeWrapper(activity, 0)` + `applyOverrideConfiguration` em vez
 * de `createConfigurationContext`: assim a Activity continua acessível na cadeia de baseContext
 * (necessário para `hiltViewModel()` e afins), e o tema/cores são herdados.
 *
 * Também atualiza `Locale.setDefault` para que formatadores java.time que usam o locale padrão
 * (datas, meses) acompanhem o idioma.
 */
@Composable
fun ProvideAppLocale(
    language: AppLanguage,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val localizedContext = remember(language, context) {
        val locale = Locale.forLanguageTag(language.tag)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration).apply {
            setLocale(locale)
        }
        ContextThemeWrapper(context, 0).apply { applyOverrideConfiguration(config) }
    }

    CompositionLocalProvider(
        LocalContext provides localizedContext,
        LocalConfiguration provides localizedContext.resources.configuration,
    ) {
        content()
    }
}