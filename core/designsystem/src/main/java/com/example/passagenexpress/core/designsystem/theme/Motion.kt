package com.example.passagenexpress.core.designsystem.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf

@Immutable
data class TotemMotion(
    val easeOut: CubicBezierEasing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f),
    val easeInOut: CubicBezierEasing = CubicBezierEasing(0.65f, 0f, 0.35f, 1f),
    val durationFast: Int = 180,
    val durationBase: Int = 240,
    val durationSlow: Int = 320,
)

val LocalTotemMotion = staticCompositionLocalOf { TotemMotion() }
