package com.descanso.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Indigo = Color(0xFF4C6FFF)
private val IndigoDark = Color(0xFF8AA1FF)

private val LightColors = lightColorScheme(
    primary = Indigo,
    onPrimary = Color.White,
    secondary = Color(0xFF3F5AC4),
    background = Color(0xFFF6F7FB),
    surface = Color.White,
)

private val DarkColors = darkColorScheme(
    primary = IndigoDark,
    onPrimary = Color(0xFF001A6B),
    secondary = Color(0xFFB9C4FF),
    background = Color(0xFF11141C),
    surface = Color(0xFF1A1F2B),
)

@Composable
fun DescansoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
