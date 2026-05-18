package com.example.passagenexpress.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.passagenexpress.core.designsystem.theme.TotemPalette
import com.example.passagenexpress.core.designsystem.theme.TotemTheme

@Composable
fun TotemPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leading: (@Composable () -> Unit)? = null,
    trailingArrow: Boolean = true,
    accent: Boolean = false,
) {
    val container = if (accent) TotemPalette.Accent else TotemPalette.Ink
    val disabled = TotemPalette.PaperDim
    val onContent = TotemPalette.Paper
    val onDisabled = TotemPalette.InkSoft
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = TotemTheme.dimens.ctaHeight),
        shape = RoundedCornerShape(TotemTheme.dimens.radiusPill),
        contentPadding = PaddingValues(
            horizontal = TotemTheme.dimens.space36,
            vertical = TotemTheme.dimens.space16,
        ),
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = onContent,
            disabledContainerColor = disabled,
            disabledContentColor = onDisabled,
        ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space12),
        ) {
            if (leading != null) leading()
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            )
            if (trailingArrow && enabled) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                )
            }
        }
    }
}

@Composable
fun TotemSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = TotemTheme.dimens.touchTarget),
        contentPadding = PaddingValues(
            horizontal = TotemTheme.dimens.space12,
            vertical = TotemTheme.dimens.space12,
        ),
        colors = ButtonDefaults.textButtonColors(
            contentColor = TotemPalette.InkMuted,
        ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space8),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
            )
            Text(
                text = text,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium),
            )
        }
    }
}

@Preview(name = "Primary button", widthDp = 320, heightDp = 80)
@Composable
private fun TotemPrimaryButtonPreview() {
    TotemTheme {
        Row(modifier = Modifier.padding(16.dp)) {
            TotemPrimaryButton(text = "Continuar", onClick = {})
        }
    }
}

@Preview(name = "Secondary button", widthDp = 320, heightDp = 80)
@Composable
private fun TotemSecondaryButtonPreview() {
    TotemTheme {
        Row(modifier = Modifier.padding(16.dp)) {
            TotemSecondaryButton(text = "Voltar", onClick = {})
        }
    }
}
