package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LuckyDarkColorScheme =
  darkColorScheme(
    primary = LuckyGreenPrimary,
    onPrimary = Color(0xFF031F0E),
    primaryContainer = LuckyGreenContainer,
    onPrimaryContainer = LuckyGreenOnContainer,
    secondary = LuckyGoldSecondary,
    onSecondary = Color(0xFF261900),
    secondaryContainer = LuckyGoldContainer,
    onSecondaryContainer = LuckyGoldOnContainer,
    tertiary = LuckyOrangeTertiary,
    onTertiary = Color(0xFF2E1102),
    tertiaryContainer = LuckyOrangeContainer,
    onTertiaryContainer = LuckyOrangeOnContainer,
    error = LuckyOrangeTertiary,
    onError = Color(0xFF2E1102),
    errorContainer = LuckyOrangeContainer,
    onErrorContainer = LuckyOrangeOnContainer,
    background = LuckyBlackBg,
    onBackground = LuckyTextPrimary,
    surface = LuckyBlackSurface,
    onSurface = LuckyTextPrimary,
    surfaceVariant = LuckyBlackElevated,
    onSurfaceVariant = LuckyTextSecondary,
    outline = LuckyBlackOutline,
  )

private val LuckyLightColorScheme =
  lightColorScheme(
    primary = LuckyGreenDeep,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD0F5DE),
    onPrimaryContainer = Color(0xFF06361A),
    secondary = LuckyGoldDeep,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFEF0C7),
    onSecondaryContainer = Color(0xFF3F2A00),
    tertiary = LuckyOrangeDeep,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE4D1),
    onTertiaryContainer = Color(0xFF4A1803),
    error = LuckyOrangeDeep,
    onError = Color.White,
    errorContainer = Color(0xFFFFE4D1),
    onErrorContainer = Color(0xFF4A1803),
    background = LightLuckyBg,
    onBackground = LightLuckyInk,
    surface = LightLuckySurface,
    onSurface = LightLuckyInk,
    surfaceVariant = LightLuckyVariant,
    onSurfaceVariant = LightLuckyInkSecondary,
    outline = LightLuckyOutline,
  )

val LuckyBoxShapes =
  Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
  )

@Composable
fun LuckyBoxTheme(
  darkTheme: Boolean = true,
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) LuckyDarkColorScheme else LuckyLightColorScheme

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    shapes = LuckyBoxShapes,
    content = content,
  )
}
