package com.example.passagenexpress.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class TotemDimens(
    val space2: Dp = 2.dp,
    val space4: Dp = 4.dp,
    val space6: Dp = 6.dp,
    val space8: Dp = 8.dp,
    val space12: Dp = 12.dp,
    val space14: Dp = 14.dp,
    val space16: Dp = 16.dp,
    val space20: Dp = 20.dp,
    val space24: Dp = 24.dp,
    val space28: Dp = 28.dp,
    val space32: Dp = 32.dp,
    val space36: Dp = 36.dp,
    val space40: Dp = 40.dp,
    val space48: Dp = 48.dp,
    val space64: Dp = 64.dp,
    val radiusSm: Dp = 12.dp,
    val radius: Dp = 18.dp,
    val radiusLg: Dp = 22.dp,
    val radiusXl: Dp = 24.dp,
    val radiusPill: Dp = 999.dp,
    val touchTarget: Dp = 64.dp,
    val touchTargetLarge: Dp = 96.dp,
    val hairline: Dp = 1.dp,
    val borderWide: Dp = 1.5.dp,
    val statusBarHeight: Dp = 72.dp,
    val footerHeight: Dp = 96.dp,
    val stagePaddingH: Dp = 36.dp,
    val stagePaddingV: Dp = 24.dp,
    val ctaHeight: Dp = 64.dp,
    val pillHeight: Dp = 48.dp,
)

val LocalTotemDimens = staticCompositionLocalOf { TotemDimens() }
