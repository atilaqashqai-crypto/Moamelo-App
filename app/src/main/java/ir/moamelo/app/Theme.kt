package ir.moamelo.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

class Palette(
    val dark: Boolean,
    val bg: Color,
    val cardBg: Color,
    val soft: Color,
    val brand: Color,
    val onBrand: Color,
    val ink: Color,
    val muted: Color,
    val border: Color,
    val green: Color,
    val warn: Color,
    val danger: Color
)

// White & green (day)
val LightPalette = Palette(
    dark = false,
    bg = Color(0xFFF4F8F5),
    cardBg = Color(0xFFFFFFFF),
    soft = Color(0xFFE4F1E9),
    brand = Color(0xFF0F7B54),
    onBrand = Color(0xFFFFFFFF),
    ink = Color(0xFF12241B),
    muted = Color(0xFF5F7569),
    border = Color(0xFFDCE7E0),
    green = Color(0xFF1E9E63),
    warn = Color(0xFFB26A00),
    danger = Color(0xFFD64545)
)

// Navy & gold (night)
val DarkPalette = Palette(
    dark = true,
    bg = Color(0xFF0B1226),
    cardBg = Color(0xFF141C38),
    soft = Color(0xFF1C2647),
    brand = Color(0xFFD9A441),
    onBrand = Color(0xFF0B1226),
    ink = Color(0xFFF7F1E6),
    muted = Color(0xFF8A93B2),
    border = Color(0xFF2A3560),
    green = Color(0xFF3FB68B),
    warn = Color(0xFFF0C87A),
    danger = Color(0xFFFF8A80)
)

val LocalPalette = staticCompositionLocalOf { LightPalette }

val Bg: Color
    @Composable get() = LocalPalette.current.bg
val CardBg: Color
    @Composable get() = LocalPalette.current.cardBg
val Soft: Color
    @Composable get() = LocalPalette.current.soft
val Brand: Color
    @Composable get() = LocalPalette.current.brand
val OnBrand: Color
    @Composable get() = LocalPalette.current.onBrand
val Ink: Color
    @Composable get() = LocalPalette.current.ink
val Muted: Color
    @Composable get() = LocalPalette.current.muted
val Border: Color
    @Composable get() = LocalPalette.current.border
val Green: Color
    @Composable get() = LocalPalette.current.green
val Warn: Color
    @Composable get() = LocalPalette.current.warn
val Danger: Color
    @Composable get() = LocalPalette.current.danger

@Composable
fun MoameloTheme(dark: Boolean, content: @Composable () -> Unit) {
    val p = if (dark) DarkPalette else LightPalette
    val scheme = if (dark) {
        darkColorScheme(
            primary = p.brand, onPrimary = p.onBrand, secondary = p.brand, onSecondary = p.onBrand,
            background = p.bg, onBackground = p.ink, surface = p.cardBg, onSurface = p.ink,
            surfaceVariant = p.soft, onSurfaceVariant = p.muted, outline = p.border, error = p.danger,
            primaryContainer = p.soft, onPrimaryContainer = p.brand,
            secondaryContainer = p.soft, onSecondaryContainer = p.brand,
            surfaceTint = p.brand,
            surfaceContainer = p.cardBg, surfaceContainerHigh = p.cardBg, surfaceContainerHighest = p.soft,
            surfaceContainerLow = p.cardBg, surfaceContainerLowest = p.cardBg
        )
    } else {
        lightColorScheme(
            primary = p.brand, onPrimary = p.onBrand, secondary = p.brand, onSecondary = p.onBrand,
            background = p.bg, onBackground = p.ink, surface = p.cardBg, onSurface = p.ink,
            surfaceVariant = p.soft, onSurfaceVariant = p.muted, outline = Color(0xFFB9CBC1), error = p.danger,
            primaryContainer = p.soft, onPrimaryContainer = p.brand,
            secondaryContainer = p.soft, onSecondaryContainer = p.brand,
            surfaceTint = p.brand,
            surfaceContainer = p.cardBg, surfaceContainerHigh = p.cardBg, surfaceContainerHighest = p.soft,
            surfaceContainerLow = p.cardBg, surfaceContainerLowest = p.cardBg
        )
    }
    CompositionLocalProvider(LocalPalette provides p) {
        MaterialTheme(colorScheme = scheme, typography = Typography(), content = content)
    }
}
