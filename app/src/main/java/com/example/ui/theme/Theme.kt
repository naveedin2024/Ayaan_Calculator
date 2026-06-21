package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val CosmicOnyx = Color(0xFF090A0E)
val CosmicSlate = Color(0xFF131620)
val CosmicSteel = Color(0xFF232A3B)
val LightAccentCyan = Color(0xFF00FFF0)
val DarkAccentCyan = Color(0xFF00C8FF)
val HighNeonOrange = Color(0xFFFF7B39)
val CosmicCoral = Color(0xFFFF5252)
val TextGray = Color(0xFF8E9AAA)
val HighWhite = Color(0xFFFFFFFF)

@Composable
fun CalculatorTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = LightAccentCyan,
            onPrimary = CosmicOnyx,
            secondary = HighNeonOrange,
            background = CosmicOnyx,
            surface = CosmicSlate
        ),
        content = content
    )
}
