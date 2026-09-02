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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
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
import com.example.passagenexpress.feature.idle.R

private val SECRET_CORNER_SIZE = 120.dp

@Composable
fun IdleScreen(
    onStart: () -> Unit,
    onSecretLongPress: () -> Unit,
    onSettings: () -> Unit,
    viewModel: IdleViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    IdleContent(
        state = state,
        onStart = onStart,
        onSecretLongPress = onSecretLongPress,
        onSettings = onSettings,
    )
}

@Composable
private fun IdleContent(
    state: IdleUiState,
    onStart: () -> Unit,
    onSecretLongPress: () -> Unit,
    onSettings: () -> Unit,
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
                onSettings = onSettings,
                modifier = Modifier.align(Alignment.TopCenter),
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = TotemTheme.dimens.space48),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Marca ancorada no terço superior: espaço menor acima que abaixo.
                Spacer(Modifier.weight(0.9f))
                Wordmark()
                Spacer(Modifier.height(TotemTheme.dimens.space24))
                Tagline()
                // Vão menor: puxa o CTA pra perto do meio da tela.
                Spacer(Modifier.weight(0.7f))
                IdleArrow()
                Spacer(Modifier.height(TotemTheme.dimens.space24))
                TapToStart()
                Spacer(Modifier.weight(1.4f))
            }

            IdleFoot(
                portoNome = state.portoNome,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

@Composable
private fun IdleTopBar(
    language: AppLanguage,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TotemTheme.dimens.space8),
        ) {
            LanguagePill(language = language)
            IconButton(onClick = onSettings) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = stringResource(R.string.idle_settings),
                    tint = TotemPalette.InkMuted,
                )
            }
        }
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
        withStyle(SpanStyle(color = TotemPalette.Ink, fontSize = 68.sp)) { append("Passagem\n") }
        withStyle(SpanStyle(color = TotemPalette.Accent)) { append("Express") }
    }
    Text(
        text = text,
        style = MaterialTheme.typography.displayLarge.copy(
            fontSize = 132.sp,
            lineHeight = 132.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = (-2).sp,
        ),
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun Tagline() {
    Text(
        text = stringResource(R.string.idle_tagline),
        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 22.sp, lineHeight = 30.sp),
        color = TotemPalette.InkMuted,
        textAlign = TextAlign.Center,
        modifier = Modifier.width(520.dp),
    )
}

@Composable
private fun IdleArrow() {
    val transition = rememberInfiniteTransition(label = "idleArrow")
    // Respiro do círculo principal.
    val pulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = TotemTheme.motion.easeInOut),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "idleArrowPulse",
    )
    // Anel "radar" que expande e some, chamando atenção.
    val ringScale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = TotemTheme.motion.easeInOut),
            repeatMode = RepeatMode.Restart,
        ),
        label = "idleArrowRingScale",
    )
    val ringAlpha by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = TotemTheme.motion.easeInOut),
            repeatMode = RepeatMode.Restart,
        ),
        label = "idleArrowRingAlpha",
    )
    // Seta cutucando para a direita, no sentido do avanço.
    val nudge by transition.animateFloat(
        initialValue = 0f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = TotemTheme.motion.easeInOut),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "idleArrowNudge",
    )
    Box(
        modifier = Modifier.size(128.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .scale(ringScale)
                .alpha(ringAlpha)
                .border(2.dp, TotemPalette.Accent, CircleShape),
        )
        Box(
            modifier = Modifier
                .size(88.dp)
                .scale(pulse)
                .border(3.dp, TotemPalette.Accent, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = TotemPalette.Accent,
                modifier = Modifier
                    .size(40.dp)
                    .offset(x = nudge.dp),
            )
        }
    }
}

@Composable
private fun TapToStart() {
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
    Text(
        text = stringResource(R.string.idle_tap_to_start).uppercase(),
        style = MaterialTheme.typography.labelLarge.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.5.sp,
            fontSize = 22.sp,
        ),
        color = TotemPalette.Accent,
        modifier = Modifier.alpha(alpha),
    )
}

@Composable
private fun IdleFoot(portoNome: String, modifier: Modifier = Modifier) {
    val label = portoNome.ifEmpty { stringResource(R.string.idle_terminal_fallback) }
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
            onSettings = {},
        )
    }
}
