package ir.moamelo.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

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

// ---- Sahel font ----
val Sahel = FontFamily(
    Font(R.font.sahel_regular, FontWeight.Normal),
    Font(R.font.sahel_bold, FontWeight.Bold)
)

private val Base = Typography()
val AppTypography = Typography(
    displayLarge = Base.displayLarge.copy(fontFamily = Sahel),
    displayMedium = Base.displayMedium.copy(fontFamily = Sahel),
    displaySmall = Base.displaySmall.copy(fontFamily = Sahel),
    headlineLarge = Base.headlineLarge.copy(fontFamily = Sahel),
    headlineMedium = Base.headlineMedium.copy(fontFamily = Sahel),
    headlineSmall = Base.headlineSmall.copy(fontFamily = Sahel),
    titleLarge = Base.titleLarge.copy(fontFamily = Sahel),
    titleMedium = Base.titleMedium.copy(fontFamily = Sahel),
    titleSmall = Base.titleSmall.copy(fontFamily = Sahel),
    bodyLarge = Base.bodyLarge.copy(fontFamily = Sahel),
    bodyMedium = Base.bodyMedium.copy(fontFamily = Sahel),
    bodySmall = Base.bodySmall.copy(fontFamily = Sahel),
    labelLarge = Base.labelLarge.copy(fontFamily = Sahel),
    labelMedium = Base.labelMedium.copy(fontFamily = Sahel),
    labelSmall = Base.labelSmall.copy(fontFamily = Sahel)
)

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
    MaterialTheme(colorScheme = Scheme, typography = AppTypography, content = content)
}
