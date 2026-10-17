package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Extension function for diplomatic/sophisticated translation of texts.
 */
fun String.diplomatic(isDiplomatico: Boolean): String {
    return this
}

@Composable
fun MyApplicationTheme(
    themeChoice: String = "azul_tech",
    fontChoice: String = "sans_grotesk",
    textColorChoice: String = "blanco",
    modoDiplomaticoChoice: String = "false",
    isLightModeChoice: String = "false",
    content: @Composable () -> Unit,
) {
    val actualTheme = if (modoDiplomaticoChoice == "true") "diplomatico" else themeChoice
    val actualFont = if (modoDiplomaticoChoice == "true") "academia_serif" else fontChoice
    val isLight = isLightModeChoice == "true"
    
    // Dynamically update the global state in Color.kt
    isSystemInLightMode = isLight

    // Maps custom text colors ensuring visible high-contrast mapping for both dark and light backdrops
    val onSurfaceColor = if (isLight) {
        when (textColorChoice) {
            "sistema" -> Color(0xFF0F1A2C)         // Deep dark navy black
            "blanco" -> Color(0xFF1E293B)          // Slate Charcoal (instead of white, so it's visible!)
            "ambar_calido" -> Color(0xFFB45309)    // Strong dark rich Amber
            "verde_claro" -> Color(0xFF15803D)     // Deep rich Emerald Green
            "cian_electrico" -> Color(0xFF0369A1)  // Strong Electric Azure Cyan
            "amarillo_crema" -> Color(0xFF854D0E)  // Golden ochre / cream brown
            "amarillo_fuerte" -> Color(0xFFB45309) // Strong Dark Gold Yellow
            "rojo_fuerte" -> Color(0xFFB91C1C)     // Strong Bold Crimson Red
            "morado_fuerte" -> Color(0xFF6D28D9)   // Deep Majestic Purple
            "rosado_fuerte" -> Color(0xFFBE185D)   // Strong Vibrant Deep Pink
            else -> Color(0xFF0F1A2C)
        }
    } else {
        when (textColorChoice) {
            "sistema" -> Color(0xFFE3E2E6)         // Cozy high-viz light grey
            "blanco" -> Color.White                // Classic pure white
            "ambar_calido" -> Color(0xFFFFCC80)    // Glowing bright warm amber
            "verde_claro" -> Color(0xFFA5D6A7)     // Soft mint green
            "cian_electrico" -> Color(0xFF80DEEA)  // Electric neon cyan
            "amarillo_crema" -> Color(0xFFFFF59D)  // Comfort butter cream yellow
            "amarillo_fuerte" -> Color(0xFFFFEE58) // Vibrant bright yellow
            "rojo_fuerte" -> Color(0xFFFF5252)     // Strong neon red
            "morado_fuerte" -> Color(0xFFE040FB)   // Stunning bright neon purple
            "rosado_fuerte" -> Color(0xFFFF4081)   // Bright energetic pink
            else -> Color(0xFFE3E2E6)
        }
    }

    val colorScheme = if (isLight) {
        when (actualTheme) {
            "diplomatico" -> lightColorScheme(
                primary = Color(0xFF856404),       // Solid Diplomatic Gold
                secondary = Color(0xFF5A4010),     // Bronze Gold Accentuations
                tertiary = Color(0xFF3F3005),
                background = Color(0xFFFDFBF7),   // Rich Linen Academic Background
                surface = Color(0xFFF7F5EE),      // Soft Cream Surface
                onPrimary = Color.White,
                onSecondary = Color.White,
                onTertiary = Color.White,
                onBackground = onSurfaceColor,
                onSurface = onSurfaceColor
            )
            "esmeralda_maya" -> lightColorScheme(
                primary = Color(0xFF047857),       // Maya Rainforest Deep Emerald
                secondary = Color(0xFF065F46),     // Jungle Moss
                tertiary = Color(0xFF064E3B),
                background = Color(0xFFF0FDF4),   // Pristine Mint Leaf White
                surface = Color(0xFFD1FAE5),      // Soft Jade Accent Card
                onPrimary = Color.White,
                onSecondary = Color.White,
                onTertiary = Color.White,
                onBackground = onSurfaceColor,
                onSurface = onSurfaceColor
            )
            "puesta_sunset" -> lightColorScheme(
                primary = Color(0xFFE11D48),       // Vibrant Coral Sunset Rose
                secondary = Color(0xFFC2410C),     // Terracotta Warm Earth
                tertiary = Color(0xFF9F1239),
                background = Color(0xFFFFF1F2),   // Soft Rose Mist
                surface = Color(0xFFFFE4E6),      // Velvet Espresso Warm Card
                onPrimary = Color.White,
                onSecondary = Color.White,
                onTertiary = Color.White,
                onBackground = onSurfaceColor,
                onSurface = onSurfaceColor
            )
            "bay_pristine" -> lightColorScheme(
                primary = Color(0xFF0369A1),       // Crystal clear cerulean water
                secondary = Color(0xFF0D9488),     // Lagoon turquoise reef
                tertiary = Color(0xFF4338CA),
                background = Color(0xFFF0F9FF),   // Clear Sky Air White
                surface = Color(0xFFE0F2FE),      // Playful water ripple card
                onPrimary = Color.White,
                onSecondary = Color.White,
                onTertiary = Color.White,
                onBackground = onSurfaceColor,
                onSurface = onSurfaceColor
            )
            else -> lightColorScheme(
                primary = HondurasBluePrimary,     // Corporate Navy Blue
                secondary = Color(0xFF0284C7),     // Soft Dodger Blue
                tertiary = Color(0xFF4F46E5),
                background = Color(0xFFF8FAFC),   // Smooth Slate Cool Background
                surface = Color(0xFFFFFFFF),      // Clean white container
                onPrimary = Color.White,
                onSecondary = HondurasBluePrimary,
                onTertiary = Color.White,
                onBackground = onSurfaceColor,
                onSurface = onSurfaceColor
            )
        }
    } else {
        when (actualTheme) {
            "diplomatico" -> darkColorScheme(
                primary = Color(0xFFD4AF37), // Polished Royal Gold
                secondary = Color(0xFFF3E5AB), // Warm Cream Ivory gold
                tertiary = Color(0xFFC5A028),  // Matte Dark Gold
                background = Color(0xFF040A18), // Deep majestic diplomatic blue
                surface = Color(0xFF0D172E),    // Clean formal navy surface
                onPrimary = Color(0xFF040A18),
                onSecondary = Color.White,
                onTertiary = Color.White,
                onBackground = onSurfaceColor,
                onSurface = onSurfaceColor
            )
            "esmeralda_maya" -> darkColorScheme(
                primary = MayaEmeraldPrimary,
                secondary = MayaEmeraldSecondary,
                tertiary = MayaEmeraldTertiary,
                background = MayaRainforestBackground,
                surface = MayaRainforestSurface,
                onPrimary = Color.White,
                onSecondary = Color.White,
                onTertiary = Color.White,
                onBackground = onSurfaceColor,
                onSurface = onSurfaceColor
            )
            "puesta_sunset" -> darkColorScheme(
                primary = LencaSunsetPrimary,
                secondary = LencaSunsetSecondary,
                tertiary = LencaSunsetTertiary,
                background = LencaVulcanoBackground,
                surface = LencaVulcanoSurface,
                onPrimary = Color.White,
                onSecondary = Color.White,
                onTertiary = Color.White,
                onBackground = onSurfaceColor,
                onSurface = onSurfaceColor
            )
            "bay_pristine" -> darkColorScheme(
                primary = PristineReefPrimary,
                secondary = PristineReefSecondary,
                tertiary = PristineReefTertiary,
                background = PristineOceanBackground,
                surface = PristineOceanSurface,
                onPrimary = Color.White,
                onSecondary = Color.White,
                onTertiary = Color.White,
                onBackground = onSurfaceColor,
                onSurface = onSurfaceColor
            )
            else -> darkColorScheme(
                primary = HondurasBlueSecondary,
                secondary = HondurasCyanSkein,
                tertiary = HondurasBlueTertiary,
                background = SlateDarkBackground,
                surface = SlateDarkSurface,
                onPrimary = Color.White,
                onSecondary = SlateDarkBackground,
                onTertiary = SlateDarkBackground,
                onBackground = onSurfaceColor,
                onSurface = onSurfaceColor
            )
        }
    }

    val selectedTypography = getTypographyForChoice(actualFont)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = selectedTypography,
        content = content
    )
}
