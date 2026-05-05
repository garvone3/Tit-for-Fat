// app/src/main/java/com/weighttracker/ui/theme/Theme.kt
package com.weighttracker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Typography

// ── Palette ──────────────────────────────────────────────────────────────────
val Background    = Color(0xFF0A0A0F)
val Surface       = Color(0xFF13131A)
val SurfaceVariant = Color(0xFF1C1C26)
val CardColor     = Color(0xFF1E1E2A)
val CardBorder    = Color(0xFF2A2A38)

val AccentBlue    = Color(0xFF4C9EFF)
val AccentGreen   = Color(0xFF4ADE80)
val AccentRed     = Color(0xFFFF6B6B)
val AccentOrange  = Color(0xFFFFB347)
val AccentPurple  = Color(0xFFA78BFA)

val TextPrimary   = Color(0xFFF0F0FF)
val TextSecondary = Color(0xFF8888AA)
val TextMuted     = Color(0xFF55556A)
val Divider       = Color(0xFF252535)

// ── Color scheme ─────────────────────────────────────────────────────────────
private val DarkColors = darkColorScheme(
    primary          = AccentBlue,
    secondary        = AccentGreen,
    tertiary         = AccentPurple,
    background       = Background,
    surface          = Surface,
    surfaceVariant   = SurfaceVariant,
    onBackground     = TextPrimary,
    onSurface        = TextPrimary,
    onSurfaceVariant = TextSecondary,
    onPrimary        = Color(0xFF001F40),
    outline          = CardBorder,
    error            = AccentRed,
)

// ── Typography ────────────────────────────────────────────────────────────────
val WeightTypography = Typography(
    displayLarge = TextStyle(
        fontSize = 48.sp, fontWeight = FontWeight.Bold, color = TextPrimary, letterSpacing = (-1).sp
    ),
    displayMedium = TextStyle(
        fontSize = 36.sp, fontWeight = FontWeight.Bold, color = TextPrimary
    ),
    headlineLarge = TextStyle(
        fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary
    ),
    headlineMedium = TextStyle(
        fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary
    ),
    titleLarge = TextStyle(
        fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary
    ),
    titleMedium = TextStyle(
        fontSize = 16.sp, fontWeight = FontWeight.Medium, color = TextPrimary
    ),
    bodyLarge = TextStyle(
        fontSize = 15.sp, fontWeight = FontWeight.Normal, color = TextPrimary
    ),
    bodyMedium = TextStyle(
        fontSize = 14.sp, fontWeight = FontWeight.Normal, color = TextSecondary
    ),
    bodySmall = TextStyle(
        fontSize = 12.sp, fontWeight = FontWeight.Normal, color = TextMuted
    ),
    labelLarge = TextStyle(
        fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextSecondary
    ),
)

@Composable
fun WeightTrackerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography  = WeightTypography,
        content     = content
    )
}