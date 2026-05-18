package com.example.passagenexpress.feature.passenger.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.passagenexpress.core.designsystem.component.TotemPrimaryButton
import com.example.passagenexpress.core.designsystem.theme.TotemTheme

/**
 * Compose-native numeric keypad. Designed for totem touch input — no native IME — with
 * 64dp+ key targets to satisfy the "touch target mínimo de 64dp" UX requirement from
 * the totem instructions.
 *
 * The keypad is composed inline above the footer; the caller manages whether it is shown
 * via [visible] and which field it edits.
 */
@Composable
fun NumericKeypad(
    label: String,
    currentValue: String,
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
    onClose: () -> Unit,
    onDone: () -> Unit,
    doneLabel: String,
    closeContentDesc: String,
    deleteContentDesc: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        tonalElevation = 6.dp,
        shadowElevation = 12.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(TotemTheme.dimens.space20),
            verticalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space12),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = currentValue.ifEmpty { "—" },
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = closeContentDesc,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            KeyGrid(
                onDigit = onDigit,
                onBackspace = onBackspace,
                deleteContentDesc = deleteContentDesc,
            )
            TotemPrimaryButton(
                text = doneLabel,
                onClick = onDone,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun KeyGrid(
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
    deleteContentDesc: String,
) {
    val rows = listOf(
        listOf('1', '2', '3'),
        listOf('4', '5', '6'),
        listOf('7', '8', '9'),
    )
    Column(verticalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space8)) {
        rows.forEach { row ->
            KeypadRow {
                row.forEach { c -> DigitKey(c, onDigit, modifier = Modifier.weight(1f)) }
            }
        }
        KeypadRow {
            Spacer(Modifier.weight(1f))
            DigitKey('0', onDigit, modifier = Modifier.weight(1f))
            BackspaceKey(onBackspace, contentDescription = deleteContentDesc, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun KeypadRow(content: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space8),
    ) { content() }
}

@Composable
private fun DigitKey(digit: Char, onDigit: (Char) -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .height(72.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onDigit(digit) },
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = digit.toString(),
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun BackspaceKey(
    onBackspace: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .height(72.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onBackspace() },
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Backspace,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}
