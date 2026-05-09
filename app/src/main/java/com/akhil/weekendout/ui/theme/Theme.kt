package com.akhil.weekendout.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = Color(0xFF2E7D5C),       // forest green
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB8EBC8),
    onPrimaryContainer = Color(0xFF002112),
    secondary = Color(0xFFE07A3E),     // warm coffee orange
    onSecondary = Color.White,
    background = Color(0xFFFCFBF7),
    onBackground = Color(0xFF1A1C19),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1C19),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF7CD7A6),
    onPrimary = Color(0xFF003822),
    primaryContainer = Color(0xFF155034),
    onPrimaryContainer = Color(0xFFB8EBC8),
    secondary = Color(0xFFFFB68B),
    onSecondary = Color(0xFF512300),
    background = Color(0xFF121311),
    onBackground = Color(0xFFE2E3DD),
    surface = Color(0xFF1A1C19),
    onSurface = Color(0xFFE2E3DD),
)

@Composable
fun WeekendOutTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colors = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val ctx = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colors, content = content)
}
