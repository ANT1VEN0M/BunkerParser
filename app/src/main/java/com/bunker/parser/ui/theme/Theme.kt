package com.bunker.parser.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = NeonPurple,
    onPrimary = BunkerBg,
    primaryContainer = NeonPurpleDark,
    onPrimaryContainer = TextPrimary,
    secondary = NeonCyan,
    onSecondary = BunkerBg,
    secondaryContainer = BunkerSurfaceVariant,
    onSecondaryContainer = NeonCyan,
    tertiary = NeonGreen,
    background = BunkerBg,
    onBackground = TextPrimary,
    surface = BunkerSurface,
    onSurface = TextPrimary,
    surfaceVariant = BunkerSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    error = NeonRed,
    onError = TextPrimary
)

@Composable
fun BunkerTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = BunkerBg.toArgb()
            window.navigationBarColor = BunkerBg.toArgb()
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = false
            insetsController.isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
