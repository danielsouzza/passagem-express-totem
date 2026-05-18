package com.example.passagenexpress.core.designsystem.component

import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.passagenexpress.core.designsystem.theme.TotemTheme

/**
 * Outlined input field designed for totem touch — no native IME. The field itself is just a
 * tap target; typing happens via [TotemNumericKeypad] / [TotemAlphaKeypad] which the parent
 * opens via [onClick].
 *
 * Visual feedback:
 *  - Border thickens + turns primary color when [focused] (keypad is open targeting this field)
 *  - Blinking caret rendered after the value (or by itself when empty) while focused
 *  - Border + label turn error color when [error] is set
 *
 * The caller owns the masked [value]; this component is purely visual.
 */
@Composable
fun TotemInputField(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    error: String? = null,
    focused: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
) {
    val borderColor = when {
        error != null -> MaterialTheme.colorScheme.error
        focused -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outline
    }
    val borderWidth = if (focused || error != null) TotemTheme.dimens.borderWide else TotemTheme.dimens.hairline
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space4),
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(width = borderWidth, color = borderColor),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = TotemTheme.dimens.touchTarget)
                .clickable(onClick = onClick),
        ) {
            Row(
                modifier = Modifier.padding(
                    horizontal = TotemTheme.dimens.space16,
                    vertical = TotemTheme.dimens.space12,
                ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = when {
                            error != null -> MaterialTheme.colorScheme.error
                            focused -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder ?: "",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            Text(
                                text = value,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        if (focused) BlinkingCaret()
                    }
                }
                if (trailing != null) trailing()
            }
        }
        if (error != null) {
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun BlinkingCaret() {
    val transition = rememberInfiniteTransition(label = "caret")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "caret-alpha",
    )
    Box(
        modifier = Modifier
            .padding(start = 2.dp)
            .width(2.dp)
            .height(22.dp)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = alpha)),
    )
}

@Preview(name = "Input — empty", widthDp = 360, heightDp = 120)
@Composable
private fun TotemInputFieldEmptyPreview() {
    TotemTheme {
        Box(Modifier.padding(16.dp)) {
            TotemInputField(label = "CPF", value = "", placeholder = "Toque para digitar", onClick = {})
        }
    }
}

@Preview(name = "Input — focused", widthDp = 360, heightDp = 120)
@Composable
private fun TotemInputFieldFocusedPreview() {
    TotemTheme {
        Box(Modifier.padding(16.dp)) {
            TotemInputField(label = "CPF", value = "123.456", focused = true, onClick = {})
        }
    }
}

@Preview(name = "Input — error", widthDp = 360, heightDp = 120)
@Composable
private fun TotemInputFieldErrorPreview() {
    TotemTheme {
        Box(Modifier.padding(16.dp)) {
            TotemInputField(label = "CPF", value = "123", error = "Documento inválido", onClick = {})
        }
    }
}
