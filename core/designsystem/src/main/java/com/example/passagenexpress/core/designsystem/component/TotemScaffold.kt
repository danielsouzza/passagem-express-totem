package com.example.passagenexpress.core.designsystem.component

import android.icu.lang.UCharacter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.example.passagenexpress.core.designsystem.theme.TotemPalette
import com.example.passagenexpress.core.designsystem.theme.TotemTheme

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
    footer: (@Composable () -> Unit)? = null,
    stickyBottom: (@Composable () -> Unit)? = null,
    body: @Composable () -> Unit,
) {
    val statusBar = LocalTotemStatusBar.current?.let { base ->
        if (step != null) base.copy(currentStep = step, totalSteps = totalSteps) else base
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
                }
            }
        }
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
