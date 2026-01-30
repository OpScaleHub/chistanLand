package com.example.chistanland.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = MangoOrange,
    secondary = SkyBlue,
    tertiary = PastelGreen,
    background = SkyBlue,
    surface = SoftYellow,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    onSecondary = DeepOcean,
    onTertiary = androidx.compose.ui.graphics.Color.Black,
    onBackground = DeepOcean,
    onSurface = DeepOcean,
)

@Composable
fun ChistanLandTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = LightColorScheme // We focus on a bright, child-friendly theme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
