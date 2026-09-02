package com.example.passagenexpress.core.designsystem.component

import android.content.res.Configuration
import android.view.ContextThemeWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

/**
 * Reaplica o idioma do app dentro de uma `Dialog`.
 *
 * Uma `Dialog` do Compose cria uma subcomposição com `LocalContext`/`LocalConfiguration` próprios
 * (do contexto do device, no idioma do aparelho), descartando o override de locale feito pelo
 * `ProvideAppLocale` na árvore principal. Sem isso, as strings dentro do modal sairiam no idioma
 * do dispositivo em vez do idioma selecionado no totem.
 *
 * Baseia-se em `Locale.getDefault()`, que o `ProvideAppLocale` mantém sincronizado com o idioma
 * escolhido. Use logo dentro do bloco `Dialog { ... }`, envolvendo todo o conteúdo do modal.
 */
@Composable
fun ProvideDialogLocale(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val locale = Locale.getDefault()
    val localizedContext = remember(context, locale) {
        val config = Configuration(context.resources.configuration).apply { setLocale(locale) }
        ContextThemeWrapper(context, 0).apply { applyOverrideConfiguration(config) }
    }
    CompositionLocalProvider(
        LocalContext provides localizedContext,
        LocalConfiguration provides localizedContext.resources.configuration,
        content = content,
    )
}
