package com.nexolite.homekitchen.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.nexolite.homekitchen.R

object Palette {
    val Brand = Color(0xFF1F4D3A)
    val BrandSoft = Color(0xFFE3EDE7)
    val BrandTrack = Color(0xFFC7D9CE)
    val Ink = Color(0xFF1B1F1C)
    val InkSoft = Color(0xFF3D443F)
    val Muted = Color(0xFF5B635D)
    val Ground = Color(0xFFF4F5F1)
    val Card = Color(0xFFFFFFFF)
    val Line = Color(0xFFE3E5DF)
    val Outline = Color(0xFFD5D8D1)
    val Saffron = Color(0xFFF2B441)
    val PendingBg = Color(0xFFFDF1D6)
    val PendingFg = Color(0xFF6B4A00)
    val PackedBg = Color(0xFFE2ECF8)
    val PackedFg = Color(0xFF1E4C86)
    val Danger = Color(0xFFA33A1F)
    val DangerBg = Color(0xFFFBE9E3)
}

val Figtree = FontFamily(
    Font(R.font.figtree_regular, FontWeight.Normal),
    Font(R.font.figtree_medium, FontWeight.Medium),
    Font(R.font.figtree_semibold, FontWeight.SemiBold),
    Font(R.font.figtree_bold, FontWeight.Bold)
)

val Bricolage = FontFamily(
    Font(R.font.bricolage_semibold, FontWeight.SemiBold),
    Font(R.font.bricolage_bold, FontWeight.Bold)
)

private val AppTypography = Typography(
    displaySmall = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.Bold, fontSize = 34.sp, lineHeight = 40.sp),
    headlineMedium = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 36.sp),
    headlineSmall = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 30.sp),
    titleLarge = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.Bold, fontSize = 17.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 18.sp),
    labelSmall = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 16.sp)
)

private val AppColors = lightColorScheme(
    primary = Palette.Brand,
    onPrimary = Color.White,
    primaryContainer = Palette.BrandSoft,
    onPrimaryContainer = Palette.Brand,
    secondary = Palette.Saffron,
    onSecondary = Palette.Ink,
    background = Palette.Ground,
    onBackground = Palette.Ink,
    surface = Palette.Card,
    onSurface = Palette.Ink,
    surfaceVariant = Palette.Ground,
    onSurfaceVariant = Palette.Muted,
    surfaceContainer = Palette.Card,
    surfaceContainerHigh = Palette.Card,
    surfaceContainerHighest = Palette.Card,
    surfaceContainerLow = Palette.Ground,
    outline = Palette.Outline,
    outlineVariant = Palette.Line,
    error = Palette.Danger,
    onError = Color.White
)

@Composable
fun HomeKitchenTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = AppColors, typography = AppTypography, content = content)
}
