package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val IgiColorScheme = darkColorScheme(
    primary = IgiPhosphorGreen,
    onPrimary = Color.Black,
    primaryContainer = IgiButtonBg,
    onPrimaryContainer = IgiPhosphorGreen,
    secondary = IgiStatusGreen,
    onSecondary = Color.Black,
    secondaryContainer = IgiSurfaceElevated,
    onSecondaryContainer = IgiSageText,
    tertiary = IgiAmberAccent,
    onTertiary = Color.Black,
    background = IgiBackground,
    onBackground = Color.White,
    surface = IgiCardSurfaceSolid,
    onSurface = Color.White,
    surfaceVariant = IgiSurfaceElevated,
    onSurfaceVariant = IgiSageText,
    outline = IgiPhosphorGreen,
    outlineVariant = IgiDisabledBorder,
    error = IgiDangerRed,
    onError = Color.White
)

private val IgiShapes = Shapes(
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(10.dp),
    large = RoundedCornerShape(12.dp)
)

@Composable
fun ProjectIgiTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = IgiColorScheme,
        typography = IgiTypography,
        shapes = IgiShapes,
        content = content
    )
}
