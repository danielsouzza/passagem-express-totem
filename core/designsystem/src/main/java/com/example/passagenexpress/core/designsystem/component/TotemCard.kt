package com.example.passagenexpress.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.passagenexpress.core.designsystem.theme.TotemPalette

@Composable
fun TotemCard(
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val border = if (selected) {
        BorderStroke(2.dp, TotemPalette.Ink)
    } else {
        BorderStroke(1.5.dp, TotemPalette.Hairline)
    }
    val container = TotemPalette.Paper
    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = container),
            border = border,
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            content = { content() },
        )
    } else {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = container),
            border = border,
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            content = { content() },
        )
    }
}
