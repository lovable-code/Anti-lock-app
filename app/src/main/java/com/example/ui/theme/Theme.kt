package com.example.ui.theme

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

private val MatrixDarkColorScheme = darkColorScheme(
    primary = MatrixGreen,
    onPrimary = Color.Black,
    secondary = MatrixGreenDim,
    onSecondary = Color.Black,
    tertiary = Color(0xFF38BDF8),
    background = MatrixBlack,
    surface = MatrixCard,
    onBackground = MatrixGreen,
    onSurface = MatrixGreen,
    surfaceVariant = MatrixSurfaceVariant,
    onSurfaceVariant = MatrixGreen,
    outline = MatrixGreen.copy(alpha = 0.5f),
    error = DangerRed
)

private val DarkColorScheme = darkColorScheme(
    primary = ShieldBlue,
    onPrimary = Color.White,
    secondary = EmeraldNeon,
    onSecondary = Color.Black,
    tertiary = AlertOrange,
    background = SlateDark,
    surface = SlateCard,
    onBackground = Color(0xFFECEFF1),
    onSurface = Color(0xFFECEFF1),
    error = DangerRed
)

private val LightColorScheme = lightColorScheme(
    primary = ShieldBlueLight,
    onPrimary = Color.White,
    secondary = ShieldBlue,
    onSecondary = Color.White,
    tertiary = AlertOrange,
    background = SlateLightBg,
    surface = SlateLightCard,
    onBackground = Color(0xFF263238),
    onSurface = Color(0xFF263238),
    error = DangerRed
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    isMatrixTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        isMatrixTheme -> MatrixDarkColorScheme
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
