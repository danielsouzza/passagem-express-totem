package com.example.passagenexpress.feature.bus.choice

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBoat
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.passagenexpress.core.designsystem.component.TotemScreenScaffold
import com.example.passagenexpress.core.designsystem.component.TotemSecondaryButton
import com.example.passagenexpress.core.designsystem.theme.TotemPalette
import com.example.passagenexpress.core.designsystem.theme.TotemTheme
import com.example.passagenexpress.feature.bus.R

/**
 * "Como você quer viajar?" — só aparece quando o totem vende as duas modalidades. Cada card leva
 * pro fluxo próprio: barco (fluxo hidroviário original) ou ônibus (grafo `bus`).
 */
@Composable
fun ModalChoiceScreen(
    onBoat: () -> Unit,
    onBus: () -> Unit,
    onBack: () -> Unit,
) {
    TotemScreenScaffold(
        title = stringResource(R.string.bus_choice_title),
        subtitle = stringResource(R.string.bus_choice_subtitle),
        showCancel = false,
        footer = { TotemSecondaryButton(text = stringResource(R.string.bus_back), onClick = onBack) },
    ) {
        // Pulso suave nos ícones — chama atenção pro toque (padrão guiado do totem).
        val pulse by rememberInfiniteTransition(label = "choicePulse").animateFloat(
            initialValue = 1f,
            targetValue = 1.08f,
            animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
            label = "choicePulseScale",
        )
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = TotemTheme.dimens.space24),
            horizontalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space32),
        ) {
            ChoiceCard(
                icon = Icons.Filled.DirectionsBoat,
                title = stringResource(R.string.bus_choice_boat),
                description = stringResource(R.string.bus_choice_boat_desc),
                iconScale = pulse,
                onClick = onBoat,
                modifier = Modifier.weight(1f),
            )
            ChoiceCard(
                icon = Icons.Filled.DirectionsBus,
                title = stringResource(R.string.bus_choice_bus),
                description = stringResource(R.string.bus_choice_bus_desc),
                iconScale = pulse,
                onClick = onBus,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ChoiceCard(
    icon: ImageVector,
    title: String,
    description: String,
    iconScale: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(TotemTheme.dimens.radiusLg))
            .clickable(onClick = onClick),
        color = TotemPalette.Paper,
        shape = RoundedCornerShape(TotemTheme.dimens.radiusLg),
        border = BorderStroke(2.dp, TotemPalette.Hairline),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(TotemTheme.dimens.space32),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space20, Alignment.CenterVertically),
        ) {
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .scale(iconScale)
                    .background(TotemPalette.AccentTint, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = TotemPalette.Accent, modifier = Modifier.size(104.dp))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                color = TotemPalette.Ink,
                textAlign = TextAlign.Center,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.titleMedium,
                color = TotemPalette.InkMuted,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Preview(widthDp = 1280, heightDp = 800)
@Composable
private fun ModalChoicePreview() {
    TotemTheme { ModalChoiceScreen(onBoat = {}, onBus = {}, onBack = {}) }
}
