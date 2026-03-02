package id.stargan.intikasirfnb.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF1B5E20),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFA5D6A7),
    onPrimaryContainer = Color(0xFF002204),
    secondary = Color(0xFF4E6051),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD0E5D1),
    onSecondaryContainer = Color(0xFF0B1D12),
    surface = Color(0xFFFBFDF8),
    onSurface = Color(0xFF1A1C19),
    background = Color(0xFFFBFDF8),
    onBackground = Color(0xFF1A1C19),
    error = Color(0xFFBA1A1A),
    onError = Color.White
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF89D98B),
    onPrimary = Color(0xFF00390A),
    primaryContainer = Color(0xFF005314),
    onPrimaryContainer = Color(0xFFA5D6A7),
    secondary = Color(0xFFB5CCB6),
    onSecondary = Color(0xFF203526),
    secondaryContainer = Color(0xFF364B3B),
    onSecondaryContainer = Color(0xFFD0E5D1),
    surface = Color(0xFF1A1C19),
    onSurface = Color(0xFFE2E3DD),
    background = Color(0xFF1A1C19),
    onBackground = Color(0xFFE2E3DD),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

@Composable
fun PosTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
