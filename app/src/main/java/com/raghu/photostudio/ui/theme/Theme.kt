package com.raghu.photostudio.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Teal = Color(0xFF0E7C7B)
val TealDark = Color(0xFF0A5958)
val Saffron = Color(0xFFF2A93B)
val Paper = Color(0xFFFAF7F2)
val Ink = Color(0xFF1F2A2A)

private val colors = lightColorScheme(
    primary = Teal,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCFEDEA),
    onPrimaryContainer = TealDark,
    secondary = Saffron,
    onSecondary = Ink,
    secondaryContainer = Color(0xFFFCE8C8),
    onSecondaryContainer = Color(0xFF5A3A00),
    background = Paper,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Color(0xFFEFEAE2),
    onSurfaceVariant = Color(0xFF55605F),
    error = Color(0xFFB3261E),
)

// Slightly larger text than default — easier for parents to read.
private val base = Typography()
private val typography = Typography(
    headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Bold),
    titleLarge = base.titleLarge.copy(fontSize = 24.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = base.titleMedium.copy(fontSize = 19.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = base.bodyLarge.copy(fontSize = 18.sp),
    bodyMedium = base.bodyMedium.copy(fontSize = 16.sp),
    labelLarge = base.labelLarge.copy(fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
)

@Composable
fun StudioTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, typography = typography, content = content)
}
