package com.topdawg.focusmaxxing.ui.theme

import android.app.Activity
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val ArcadeColorScheme = darkColorScheme(
    primary = ArcadeColors.Accent,
    onPrimary = ArcadeColors.Background,
    primaryContainer = ArcadeColors.AccentDim,
    onPrimaryContainer = ArcadeColors.Background,
    secondary = ArcadeColors.AccentDim,
    onSecondary = ArcadeColors.Background,
    secondaryContainer = ArcadeColors.SurfaceHigh,
    onSecondaryContainer = ArcadeColors.Text,
    tertiary = ArcadeColors.Info,
    onTertiary = ArcadeColors.Background,
    error = ArcadeColors.Danger,
    onError = ArcadeColors.Background,
    background = ArcadeColors.Background,
    onBackground = ArcadeColors.Text,
    surface = ArcadeColors.Surface,
    onSurface = ArcadeColors.Text,
    surfaceVariant = ArcadeColors.SurfaceHigh,
    onSurfaceVariant = ArcadeColors.TextMuted,
    outline = ArcadeColors.Border,
    outlineVariant = ArcadeColors.Border,
    scrim = ArcadeColors.Background
)

@Composable
fun FocusmaxxingTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = ArcadeColors.Background.toArgb()
            window.navigationBarColor = ArcadeColors.Background.toArgb()
            // Our own navigation bar colour is already dark enough; drop the system's
            // translucent contrast scrim so the area behind the bar stays flat app background.
            if (android.os.Build.VERSION.SDK_INT >= 29) window.isNavigationBarContrastEnforced = false
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = ArcadeColorScheme,
        typography = Typography,
        content = content
    )
}
