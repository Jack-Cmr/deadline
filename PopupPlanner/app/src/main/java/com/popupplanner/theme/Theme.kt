package com.popupplanner.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF8A4A2A),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDBCA),
    onPrimaryContainer = Color(0xFF351000),
    secondary = Color(0xFF75584A),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFDBCA),
    onSecondaryContainer = Color(0xFF2B160C),
    tertiary = Color(0xFF5F6135),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFE5E7AF),
    onTertiaryContainer = Color(0xFF1C1D00),
    background = Color(0xFFFFF8F5),
    onBackground = Color(0xFF221A16),
    surface = Color(0xFFFFF8F5),
    onSurface = Color(0xFF221A16),
    surfaceVariant = Color(0xFFF2DFD6),
    onSurfaceVariant = Color(0xFF51443D),
    outline = Color(0xFF83746C),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB596),
    onPrimary = Color(0xFF522300),
    primaryContainer = Color(0xFF6D371C),
    onPrimaryContainer = Color(0xFFFFDBCA),
    secondary = Color(0xFFE5BFAF),
    onSecondary = Color(0xFF422B20),
    secondaryContainer = Color(0xFF5A4136),
    onSecondaryContainer = Color(0xFFFFDBCA),
    tertiary = Color(0xFFC8CA92),
    onTertiary = Color(0xFF30320B),
    tertiaryContainer = Color(0xFF47491F),
    onTertiaryContainer = Color(0xFFE5E7AF),
    background = Color(0xFF1A120E),
    onBackground = Color(0xFFF1DFD7),
    surface = Color(0xFF1A120E),
    onSurface = Color(0xFFF1DFD7),
    surfaceVariant = Color(0xFF51443D),
    onSurfaceVariant = Color(0xFFD6C2B8),
    outline = Color(0xFF9F8D84),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

@Composable
fun DeadlineTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
