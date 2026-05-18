package com.example.passagenexpress.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.passagenexpress.core.designsystem.theme.TotemPalette
import com.example.passagenexpress.core.designsystem.theme.TotemTheme

/**
 * Generic list row matching the Ferry Kiosk `.row` pattern:
 * hairline divider, square chip on the left, primary/secondary text, optional right slot, chevron.
 */
@Composable
fun TotemListRow(
    title: String,
    subtitle: String? = null,
    selected: Boolean = false,
    enabled: Boolean = true,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
) {
    val mainColor = if (selected) TotemPalette.Accent else TotemPalette.Ink
    val chipBg = if (selected) TotemPalette.Accent else TotemPalette.AccentTint
    val chipFg = if (selected) TotemPalette.Paper else TotemPalette.Accent
    val chevron = if (selected) TotemPalette.Accent else TotemPalette.InkSoft

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.36f)
            .clickable(enabled = enabled, onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = TotemTheme.dimens.space24),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space20),
        ) {
            if (leading != null) {
                Surface(
                    color = chipBg,
                    contentColor = chipFg,
                    shape = RoundedCornerShape(TotemTheme.dimens.radiusSm),
                    modifier = Modifier.size(56.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) { leading() }
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = mainColor,
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TotemPalette.InkMuted,
                    )
                }
            }
            if (trailing != null) {
                trailing()
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = chevron,
                modifier = Modifier.size(22.dp),
            )
        }
        HorizontalDivider(color = TotemPalette.Hairline)
    }
}

@Composable
fun TotemEyebrow(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
        color = TotemPalette.Accent,
    )
}

@Composable
fun TotemRowTag(text: String, color: androidx.compose.ui.graphics.Color = TotemPalette.Accent) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
        color = color,
        modifier = Modifier.padding(start = TotemTheme.dimens.space8),
    )
}
