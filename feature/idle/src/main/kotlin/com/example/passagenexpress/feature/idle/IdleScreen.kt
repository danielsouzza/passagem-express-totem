package com.example.passagenexpress.feature.idle

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.passagenexpress.core.designsystem.theme.TotemPalette
import com.example.passagenexpress.core.designsystem.theme.TotemTheme
import com.example.passagenexpress.core.domain.model.AppLanguage

private val SECRET_CORNER_SIZE = 120.dp

@Composable
fun IdleScreen(
    onStart: () -> Unit,
    onSecretLongPress: () -> Unit,
    viewModel: IdleViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    IdleContent(
        state = state,
        onStart = onStart,
        onSecretLongPress = onSecretLongPress,
    )
}

@Composable
private fun IdleContent(
    state: IdleUiState,
    onStart: () -> Unit,
    onSecretLongPress: () -> Unit,
) {
    val cornerPx = with(LocalDensity.current) { SECRET_CORNER_SIZE.toPx() }
    Surface(modifier = Modifier.fillMaxSize(), color = TotemPalette.Paper) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(TotemPalette.Paper)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { onStart() },
                        onLongPress = { offset: Offset ->
                            if (offset.x <= cornerPx && offset.y <= cornerPx) {
                                onSecretLongPress()
                            }
                        },
                    )
                },
        ) {
            IdleTopBar(
                language = state.defaultLanguage,
                modifier = Modifier.align(Alignment.TopCenter),
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center)
                    .padding(horizontal = TotemTheme.dimens.space48),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space24),
            ) {
                Wordmark()
                Tagline(language = state.defaultLanguage)
                Spacer(Modifier.height(TotemTheme.dimens.space16))
                IdleArrow()
                TapToStart(language = state.defaultLanguage)
            }

            IdleFoot(
                portoNome = state.portoNome,
                language = state.defaultLanguage,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

@Composable
private fun IdleTopBar(language: AppLanguage, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = TotemTheme.dimens.space48,
                vertical = TotemTheme.dimens.space36,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space8),
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(TotemPalette.Accent, CircleShape),
            )
            Text(
                text = "passagens express",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                ),
                color = TotemPalette.Ink,
            )
        }
        LanguagePill(language = language)
    }
}

@Composable
private fun LanguagePill(language: AppLanguage) {
    Surface(
        color = TotemPalette.PaperDim,
        shape = RoundedCornerShape(TotemTheme.dimens.radiusPill),
    ) {
        Row(
            modifier = Modifier
                .height(TotemTheme.dimens.pillHeight)
                .padding(horizontal = TotemTheme.dimens.space16),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space8),
        ) {
            Icon(
                imageVector = Icons.Filled.Language,
                contentDescription = null,
                tint = TotemPalette.Ink,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = if (language == AppLanguage.PtBr) "PT" else "EN",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = TotemPalette.Ink,
            )
        }
    }
}

@Composable
private fun Wordmark() {
    val text = buildAnnotatedString {
        withStyle(SpanStyle(color = TotemPalette.Ink, fontSize = 100.sp)) { append("Passagem\n") }
        withStyle(SpanStyle(color = TotemPalette.Accent)) { append("Express") }
    }
    Text(
        text = text,
        style = MaterialTheme.typography.displayLarge.copy(
            fontSize = 200.sp,
            lineHeight = 200.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = (-2).sp,
        ),
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun Tagline(language: AppLanguage) {
    Text(
        text = when (language) {
            AppLanguage.PtBr -> "Compre seu bilhete e embarque em menos de um minuto."
            AppLanguage.EnUs -> "Buy your ticket and board in under a minute."
        },
        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 22.sp, lineHeight = 30.sp),
        color = TotemPalette.InkMuted,
        textAlign = TextAlign.Center,
        modifier = Modifier.width(520.dp),
    )
}

@Composable
private fun IdleArrow() {
    val transition = rememberInfiniteTransition(label = "idleArrow")
    val offsetY by transition.animateFloat(
        initialValue = 0f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = TotemTheme.motion.easeInOut),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "idleArrowOffset",
    )
    Box(
        modifier = Modifier
            .size(60.dp)
            .border(2.dp, TotemPalette.Accent, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = TotemPalette.Accent,
            modifier = Modifier
                .size(26.dp)
                .padding(top = offsetY.dp),
        )
    }
}

@Composable
private fun TapToStart(language: AppLanguage) {
    val transition = rememberInfiniteTransition(label = "idleCtaPulse")
    val alpha by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = TotemTheme.motion.easeInOut),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "idleCtaAlpha",
    )
    val label = when (language) {
        AppLanguage.PtBr -> "Toque para começar"
        AppLanguage.EnUs -> "Tap to start"
    }
    Text(
        text = label.uppercase(),
        style = MaterialTheme.typography.labelLarge.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            fontSize = 18.sp,
        ),
        color = TotemPalette.Accent,
        modifier = Modifier.alpha(alpha),
    )
}

@Composable
private fun IdleFoot(portoNome: String, language: AppLanguage, modifier: Modifier = Modifier) {
    val label = when {
        portoNome.isNotEmpty() -> portoNome
        language == AppLanguage.PtBr -> "Terminal Marítimo"
        else -> "Maritime Terminal"
    }
    Text(
        text = label.uppercase(),
        fontSize = 18.sp,
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp,
        ),
        color = TotemPalette.InkSoft,
        modifier = modifier
            .padding(
                horizontal = TotemTheme.dimens.space48,
                vertical = TotemTheme.dimens.space36,
            ),
    )
}

@Preview(name = "Idle", widthDp = 800, heightDp = 1280)
@Composable
private fun IdleScreenPreview() {
    TotemTheme {
        IdleContent(
            state = IdleUiState(portoNome = "Porto de Salvador"),
            onStart = {},
            onSecretLongPress = {},
        )
    }
}
