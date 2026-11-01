package io.github.adelledgar.ariseup.ui.theme

import android.graphics.Typeface
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Paleta del Sistema (spec §4.1). */
object SystemColors {
    val background = Color(0xFF05070D)
    val panel = Color(0xCC0B1426)
    val accent = Color(0xFF3FD8FF)
    val danger = Color(0xFFFF3B3B)
    val text = Color(0xFFE6F4FF)
    val muted = Color(0xFF7A8BA6)
    val success = Color(0xFF5CFFB0)
    val gold = Color(0xFFFFD45C)
}

@Immutable
data class SystemPalette(val accent: Color, val penalty: Boolean)

val LocalSystemPalette = staticCompositionLocalOf { SystemPalette(SystemColors.accent, penalty = false) }

/** Títulos con una sans condensada del sistema: aire técnico sin incluir fuentes de terceros. */
val TitleFont = FontFamily(Typeface.create("sans-serif-condensed", Typeface.BOLD))

private val typography = Typography(
    headlineLarge = TextStyle(fontFamily = TitleFont, fontWeight = FontWeight.Bold, fontSize = 30.sp, letterSpacing = 1.5.sp),
    headlineMedium = TextStyle(fontFamily = TitleFont, fontWeight = FontWeight.Bold, fontSize = 24.sp, letterSpacing = 1.2.sp),
    titleLarge = TextStyle(fontFamily = TitleFont, fontWeight = FontWeight.Bold, fontSize = 20.sp, letterSpacing = 1.sp),
    titleMedium = TextStyle(fontFamily = TitleFont, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, letterSpacing = 0.8.sp),
    labelLarge = TextStyle(fontFamily = TitleFont, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, letterSpacing = 1.sp),
    bodyLarge = TextStyle(fontSize = 16.sp),
    bodyMedium = TextStyle(fontSize = 14.sp),
    bodySmall = TextStyle(fontSize = 12.sp),
)

@Composable
fun AriseUpTheme(penalty: Boolean, content: @Composable () -> Unit) {
    val accent by animateColorAsState(if (penalty) SystemColors.danger else SystemColors.accent, tween(900), label = "accent")
    val scheme = darkColorScheme(
        primary = accent,
        onPrimary = SystemColors.background,
        secondary = accent,
        background = SystemColors.background,
        onBackground = SystemColors.text,
        surface = SystemColors.background,
        onSurface = SystemColors.text,
        surfaceVariant = SystemColors.panel,
        onSurfaceVariant = SystemColors.muted,
        error = SystemColors.danger,
        outline = accent,
    )
    CompositionLocalProvider(LocalSystemPalette provides SystemPalette(accent, penalty)) {
        MaterialTheme(colorScheme = scheme, typography = typography, content = content)
    }
}
