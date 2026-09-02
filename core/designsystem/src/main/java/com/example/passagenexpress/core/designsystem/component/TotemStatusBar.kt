package com.example.passagenexpress.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.passagenexpress.core.designsystem.R
import com.example.passagenexpress.core.designsystem.theme.TotemPalette
import com.example.passagenexpress.core.designsystem.theme.TotemTheme

enum class TotemLanguageOption { Pt, En }

@Immutable
data class TotemStatusBarState(
    val brand: String,
    val portoNome: String,
    val time: String,
    val language: TotemLanguageOption,
    val onLanguageChange: (TotemLanguageOption) -> Unit,
    val currentStep: Int? = null,
    val totalSteps: Int = 6,
    val onHomeClick: (() -> Unit)? = null,
    val onFilterClick: (() -> Unit)? = null,
)

val LocalTotemStatusBar = staticCompositionLocalOf<TotemStatusBarState?> { null }

@Composable
fun TotemStatusBar(state: TotemStatusBarState, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top=TotemTheme.dimens.space24)
                .padding(
                    horizontal = TotemTheme.dimens.stagePaddingH
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            if (state.currentStep != null) {
                StepPill(current = state.currentStep, total = state.totalSteps)
            } else {
                LocationPill(portoNome = state.portoNome, time = state.time)
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space8),
            ) {
                if (state.onHomeClick != null) {
                    CircleActionButton(
                        icon = Icons.Filled.Home,
                        description = "Início",
                        onClick = state.onHomeClick,
                    )
                }
                if (state.onFilterClick != null) {
                    CircleActionButton(
                        icon = Icons.Filled.FilterAlt,
                        description = "Filtros",
                        onClick = state.onFilterClick,
                    )
                }
                LanguageToggle(
                    language = state.language,
                    onLanguageChange = state.onLanguageChange,
                )
            }
        }
    }
}

@Composable
private fun CircleActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
) {
    Surface(
        color = TotemPalette.AccentTint,
        shape = CircleShape,
        modifier = Modifier
            .size(TotemTheme.dimens.pillHeight)
            .clip(CircleShape),
    ) {
        IconButton(onClick = onClick) {
            Icon(
                imageVector = icon,
                contentDescription = description,
                tint = TotemPalette.Ink,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun StepPill(current: Int, total: Int) {
    Surface(
        color = TotemPalette.AccentTint,
        shape = RoundedCornerShape(TotemTheme.dimens.radiusPill),
    ) {
        Row(
            modifier = Modifier
                .height(TotemTheme.dimens.pillHeight)
                .padding(horizontal = TotemTheme.dimens.space20),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space8),
        ) {
            Text(
                text = stringResource(R.string.totem_step_pill_label),
                fontSize = 17.sp,
                style = MaterialTheme.typography.labelSmall,
                color = TotemPalette.InkMuted,
            )
            Text(
                fontSize = 17.sp,
                text = current.toString(),
                style = MaterialTheme.typography.titleSmall,
                color = TotemPalette.InkMuted,
            )
            Text(
                fontSize = 17.sp,
                text = stringResource(R.string.totem_step_pill_of),
                style = MaterialTheme.typography.labelSmall,
                color = TotemPalette.InkMuted,
            )
            Text(
                fontSize = 17.sp,
                text = total.toString(),
                style = MaterialTheme.typography.titleSmall,
                color = TotemPalette.InkMuted,
            )
        }
    }
}

@Composable
private fun LocationPill(portoNome: String, time: String) {
    Surface(
        color = TotemPalette.AccentTint,
        shape = RoundedCornerShape(TotemTheme.dimens.radiusPill),
    ) {
        Row(
            modifier = Modifier
                .height(TotemTheme.dimens.pillHeight)
                .padding(horizontal = TotemTheme.dimens.space20),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space12),
        ) {
            Icon(
                imageVector = Icons.Filled.LocationOn,
                contentDescription = null,
                tint = TotemPalette.Ink,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = portoNome.ifEmpty { "—" },
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium),
                color = TotemPalette.Ink,
            )
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .background(TotemPalette.InkSoft, CircleShape),
            )
            Text(
                text = time,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium),
                color = TotemPalette.InkMuted,
            )
        }
    }
}

@Composable
private fun LanguageToggle(
    language: TotemLanguageOption,
    onLanguageChange: (TotemLanguageOption) -> Unit,
) {
    Surface(
        color = TotemPalette.AccentTint,
        shape = RoundedCornerShape(TotemTheme.dimens.radiusPill),
    ) {
        Row(
            modifier = Modifier
                .height(TotemTheme.dimens.pillHeight)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LanguagePill(
                label = "PT",
                active = language == TotemLanguageOption.Pt,
                onClick = { onLanguageChange(TotemLanguageOption.Pt) },
            )
            Spacer(Modifier.width(2.dp))
            LanguagePill(
                label = "EN",
                active = language == TotemLanguageOption.En,
                onClick = { onLanguageChange(TotemLanguageOption.En) },
            )
        }
    }
}

@Composable
private fun LanguagePill(label: String, active: Boolean, onClick: () -> Unit) {
    val container = if (active) TotemPalette.Accent else Color.Transparent
    val content = if (active) TotemPalette.Paper else TotemPalette.InkMuted
    Surface(
        color = container,
        contentColor = content,
        shape = CircleShape,
        modifier = Modifier
            .clip(CircleShape)
            .clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .height(40.dp)
                .padding(horizontal = TotemTheme.dimens.space16),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            )
        }
    }
}

@Preview(name = "Totem status bar", widthDp = 800, heightDp = 96)
@Composable
private fun TotemStatusBarPreview() {
    TotemTheme {
        TotemStatusBar(
            state = TotemStatusBarState(
                brand = "Passagens Express",
                portoNome = "Term. Marítimo",
                time = "18:10",
                language = TotemLanguageOption.Pt,
                onLanguageChange = {},
            ),
        )
    }
}
