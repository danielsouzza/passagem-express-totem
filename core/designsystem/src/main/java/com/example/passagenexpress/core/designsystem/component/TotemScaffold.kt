package com.example.passagenexpress.core.designsystem.component

import android.icu.lang.UCharacter
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.passagenexpress.core.designsystem.R
import com.example.passagenexpress.core.designsystem.theme.TotemPalette
import com.example.passagenexpress.core.designsystem.theme.TotemTheme

/**
 * Ação de cancelar a compra e voltar ao início. Provida pelo chrome (`TotemAppChrome`) e
 * consumida por [TotemScreenScaffold], que desenha um botão sutil no rodapé de toda flow screen.
 * null = sem cancelar (ex.: fora do chrome).
 */
val LocalTotemCancelAction = staticCompositionLocalOf<(() -> Unit)?> { null }

/**
 * Wraps a screen with optional title/subtitle header + body + sticky footer slot.
 *
 * `step` (e.g. `2`) sets the step counter in the status-bar pill (rendered as `ETAPA 2 de 6`).
 * `eyebrow` is a fallback inline label below the title; if `step` is set, the eyebrow is
 * not rendered inline since the same information lives in the top bar.
 */
@Composable
fun TotemScreenScaffold(
    modifier: Modifier = Modifier,
    title: String? = null,
    subtitle: String? = null,
    eyebrow: String? = null,
    step: Int? = null,
    totalSteps: Int = 6,
    onFilterClick: (() -> Unit)? = null,
    showCancel: Boolean = true,
    footer: (@Composable () -> Unit)? = null,
    stickyBottom: (@Composable () -> Unit)? = null,
    body: @Composable () -> Unit,
) {
    val cancelAction = LocalTotemCancelAction.current
    val statusBar = LocalTotemStatusBar.current?.let { base ->
        var merged = base
        if (step != null) merged = merged.copy(currentStep = step, totalSteps = totalSteps)
        if (onFilterClick != null) merged = merged.copy(onFilterClick = onFilterClick)
        merged
    }
    val showInlineEyebrow = eyebrow != null && step == null
    Surface(
        modifier = modifier.fillMaxSize(),
        color = TotemPalette.Paper,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (statusBar != null) {
                TotemStatusBar(state = statusBar)
            }
            if (showInlineEyebrow || title != null) {
                ScaffoldHeader(
                    eyebrow = if (showInlineEyebrow) eyebrow else null,
                    title = title,
                    subtitle = subtitle,
                )

            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = TotemTheme.dimens.stagePaddingH),
                contentAlignment = Alignment.TopStart,
            ) { body() }
            if (stickyBottom != null) {
                Box(modifier = Modifier.fillMaxWidth()) { stickyBottom() }
            }
            if (footer != null) {
                HorizontalDivider(color = TotemPalette.Hairline)
                Surface(color = TotemPalette.Paper) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = TotemTheme.dimens.stagePaddingH,
                                    vertical = TotemTheme.dimens.space20,
                                ),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space20),
                        ) { footer() }
                        // Cancelar fica no centro da barra, entre Voltar (esquerda) e Avançar (direita).
                        if (showCancel && cancelAction != null) {
                            FooterCancelButton(
                                onClick = cancelAction,
                                modifier = Modifier.align(Alignment.Center),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Botão discreto de cancelar a compra, centralizado na barra de navegação (entre Voltar/Avançar). */
@Composable
private fun FooterCancelButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(TotemTheme.dimens.radiusPill),
        border = BorderStroke(1.5.dp, TotemPalette.Ink2),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = TotemPalette.Ink2),
    ) {
        Icon(
            imageVector = Icons.Filled.Close,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(TotemTheme.dimens.space8))
        Text(
            text = stringResource(R.string.totem_cancel),
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
        )
    }
}

@Composable
private fun ScaffoldHeader(eyebrow: String?, title: String?, subtitle: String?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = TotemTheme.dimens.stagePaddingH,
                vertical = TotemTheme.dimens.space24,
            ),
        verticalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space8),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (eyebrow != null) {
            Text(
                text = eyebrow.uppercase(),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = TotemPalette.Accent,
                textAlign = TextAlign.Center,
            )
        }
        if (title != null) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge,
                color = TotemPalette.Ink,
                textAlign = TextAlign.Center,
            )
        }
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = TotemPalette.InkMuted,
                textAlign = TextAlign.Center,
            )
        }
        HorizontalDivider(color = TotemPalette.Hairline, modifier = Modifier.padding(vertical = TotemTheme.dimens.space20))
    }
}
