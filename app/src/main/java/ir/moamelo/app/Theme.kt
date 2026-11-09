package ir.moamelo.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ---- White & green palette ----
val Bg = Color(0xFFF4F8F5)        // screen background (very light green-white)
val CardBg = Color(0xFFFFFFFF)    // cards
val Soft = Color(0xFFE4F1E9)      // light green tint (unselected chips, containers)
val Brand = Color(0xFF0F7B54)     // main green
val OnBrand = Color(0xFFFFFFFF)   // text/icons on green
val Ink = Color(0xFF12241B)       // main text
val Muted = Color(0xFF5F7569)     // secondary text
val Border = Color(0xFFDCE7E0)
val Green = Color(0xFF1E9E63)     // success
val Warn = Color(0xFFB26A00)      // warnings
val Danger = Color(0xFFD64545)

private val Scheme = lightColorScheme(
    primary = Brand,
    onPrimary = OnBrand,
    secondary = Brand,
    onSecondary = OnBrand,
    background = Bg,
    onBackground = Ink,
    surface = CardBg,
    onSurface = Ink,
    surfaceVariant = Soft,
    onSurfaceVariant = Muted,
    outline = Color(0xFFB9CBC1),
    error = Danger,
    primaryContainer = Soft,
    onPrimaryContainer = Brand,
    secondaryContainer = Soft,
    onSecondaryContainer = Brand,
    surfaceTint = Brand,
    surfaceContainer = CardBg,
    surfaceContainerHigh = CardBg,
    surfaceContainerHighest = Soft,
    surfaceContainerLow = CardBg,
    surfaceContainerLowest = CardBg
)

@Composable
fun MoameloTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, typography = Typography(), content = content)
}
