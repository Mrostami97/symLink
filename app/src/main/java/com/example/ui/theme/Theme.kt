package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

val LocalIsDarkTheme = staticCompositionLocalOf { true }
val LocalIsPersianRtl = staticCompositionLocalOf { true }

/**
 * Full Material 3 Obsidian Midnight Dark ColorScheme (complete 30+ M3 token set).
 */
private val ObsidianDarkColorScheme = darkColorScheme(
    primary = SignalCyan,
    onPrimary = OnSignalCyan,
    primaryContainer = SignalCyanContainer,
    onPrimaryContainer = OnSignalCyanContainer,
    inversePrimary = SignalCyanDark,
    secondary = Sim2Amber,
    onSecondary = OnSim2Amber,
    secondaryContainer = Sim2AmberContainer,
    onSecondaryContainer = OnSim2AmberContainer,
    tertiary = ConnectedEmerald,
    onTertiary = OnConnectedEmerald,
    tertiaryContainer = ConnectedEmeraldContainer,
    onTertiaryContainer = OnConnectedEmeraldContainer,
    background = ObsidianBackground,
    onBackground = TextPrimaryDark,
    surface = ObsidianSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = ObsidianSurfaceContainer,
    onSurfaceVariant = TextSecondaryDark,
    surfaceTint = SignalCyan,
    inverseSurface = InverseSurfaceDark,
    inverseOnSurface = InverseOnSurfaceDark,
    surfaceDim = ObsidianSurfaceDim,
    surfaceBright = ObsidianSurfaceBright,
    surfaceContainerLowest = ObsidianBackground,
    surfaceContainerLow = ObsidianSurface,
    surfaceContainer = ObsidianSurfaceContainer,
    surfaceContainerHigh = ObsidianSurfaceHigh,
    surfaceContainerHighest = ObsidianSurfaceHighest,
    outline = ObsidianBorder,
    outlineVariant = ObsidianBorderSubtle,
    scrim = ObsidianScrim,
    error = AlertCrimson,
    onError = OnAlertCrimson,
    errorContainer = AlertCrimsonContainer,
    onErrorContainer = OnAlertCrimsonContainer
)

/**
 * Full Material 3 High-Contrast Daylight Light ColorScheme (complete 30+ M3 token set).
 * Tuned for WCAG AA/AAA legibility in both Persian RTL and English LTR layouts.
 */
private val SimLinkLightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    inversePrimary = SignalCyan,
    secondary = LightSecondary,
    onSecondary = LightOnSecondary,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = LightOnSecondaryContainer,
    tertiary = LightTertiary,
    onTertiary = LightOnTertiary,
    tertiaryContainer = LightTertiaryContainer,
    onTertiaryContainer = LightOnTertiaryContainer,
    background = LightBackground,
    onBackground = TextPrimaryLight,
    surface = LightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = LightSurfaceContainer,
    onSurfaceVariant = TextSecondaryLight,
    surfaceTint = LightPrimary,
    inverseSurface = InverseSurfaceLight,
    inverseOnSurface = InverseOnSurfaceLight,
    surfaceDim = LightSurfaceDim,
    surfaceBright = LightSurfaceBright,
    surfaceContainerLowest = LightSurface,
    surfaceContainerLow = LightSurfaceContainerLow,
    surfaceContainer = LightSurfaceContainer,
    surfaceContainerHigh = LightSurfaceHigh,
    surfaceContainerHighest = LightSurfaceHighest,
    outline = LightBorder,
    outlineVariant = LightBorderSubtle,
    scrim = ObsidianScrim,
    error = LightError,
    onError = LightOnError,
    errorContainer = LightErrorContainer,
    onErrorContainer = LightOnErrorContainer
)

val ITellShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

/**
 * Material 3 theme for SimLink with explicit manual Light/Dark override.
 * - Forces Dark (ObsidianDarkColorScheme) or Light (SimLinkLightColorScheme) regardless of system settings.
 * - Dynamically switches system status & navigation bar icon contrast.
 * - Adapts typography metrics automatically for Persian RTL readability vs English LTR.
 */
@Composable
fun ITellTheme(
    darkTheme: Boolean = true,
    isPersian: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) ObsidianDarkColorScheme else SimLinkLightColorScheme
    val typography = typographyForLanguage(isPersian = isPersian)
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(
        LocalIsDarkTheme provides darkTheme,
        LocalIsPersianRtl provides isPersian
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            shapes = ITellShapes,
            content = content
        )
    }
}
