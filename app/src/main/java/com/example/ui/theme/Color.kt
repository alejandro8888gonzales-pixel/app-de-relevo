package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// ==========================================
// 1. CLÁSICO TECNOLÓGICO HND (Premium Slate Dark Theme) - Adaptable to Light Mode
// ==========================================
var isSystemInLightMode: Boolean = false

val HondurasBluePrimary: Color
    get() = if (isSystemInLightMode) Color(0xFF0F2D59) else Color(0xFF3880EC)

val HondurasBlueSecondary: Color
    get() = if (isSystemInLightMode) Color(0xFF1565C0) else Color(0xFF1E90FF) // Gorgeous Electric Dodger Blue

val HondurasBlueTertiary: Color
    get() = if (isSystemInLightMode) Color(0xFF4527A0) else Color(0xFFB388FF) // Nebula Violet

val HondurasBlueLogo = Color(0xFF0C192E) // Keep always dark elegant navy so headers remain crisp and white icons stand out

val HondurasCyanSkein: Color
    get() = if (isSystemInLightMode) Color(0xFF007A87) else Color(0xFF00E5FF) // Rich teal in light mode / glowing aurora cyan in dark

val CoralAccent: Color
    get() = if (isSystemInLightMode) Color(0xFFC62828) else Color(0xFFFF5252) // Soft Vibrant Coral Red

val GoldRank: Color
    get() = if (isSystemInLightMode) Color(0xFF9A6E00) else Color(0xFFFFD700) // Polished Gold


// Premium Space Charcoal Atmosphere
val SlateDarkBackground = Color(0xFF080C14)    // Rich deep-space blue-black
val SlateDarkSurface = Color(0xFF121A2A)       // Elegant charcoal card surface
val SlateDarkCard = Color(0xFF1F2D47)          // Lighter soft carbon slate card

// ==========================================
// 2. ESMERALDA MAYA (Aura Jade & Deep Pine Theme)
// ==========================================
val MayaEmeraldPrimary = Color(0xFF10B981)     // Premium Jade Emerald
val MayaEmeraldSecondary = Color(0xFF34D399)   // Mint Aurora Glow
val MayaEmeraldTertiary = Color(0xFF059669)    // Deep Forest Pine
val MayaEmeraldAccent = Color(0xFFFFC107)      // Refined Matte Gold

// Rainforest Midnight Atmosphere
val MayaRainforestBackground = Color(0xFF021D18) // Mysterious jungle depth
val MayaRainforestSurface = Color(0xFF09312A)    // Elegant dark moss card
val MayaRainforestCard = Color(0xFF154E43)       // Soft forest teal card

// ==========================================
// 3. PUESTA DE SOL LENCA (Volcano Sunset & Terracotta Dark)
// ==========================================
val LencaSunsetPrimary = Color(0xFFF43F5E)     // Vibrant Coral Rose
val LencaSunsetSecondary = Color(0xFFFB923C)   // Warm Sunset Orange
val LencaSunsetTertiary = Color(0xFFE11D48)    // Intense Volcano Red
val LencaSunsetAccent = Color(0xFFFBBF24)      // Golden Amber Sunset

// Velvet Obsidian Atmosphere
val LencaVulcanoBackground = Color(0xFF0C0607) // Deep obsidian with warm tint
val LencaVulcanoSurface = Color(0xFF1A1011)    // Warm espresso surface
val LencaVulcanoCard = Color(0xFF2D1E20)       // Mild copper rust card

// ==========================================
// 4. BAY PRISTINE REEF (Roatán Azure & Purple Nebula Theme)
// ==========================================
val PristineReefPrimary = Color(0xFF0EA5E9)   // Deep Sky Cerulean
val PristineReefSecondary = Color(0xFF14B8A6) // Clear Lagoon Turquoise
val PristineReefTertiary = Color(0xFF8B5CF6)  // Cosmic Purple Lavender
val PristineReefAccent = Color(0xFFF1F5F9)    // Pearl White Coral

// Pristine Ocean Twilight Atmosphere
val PristineOceanBackground = Color(0xFF040B14) // Abyssal ocean blue
val PristineOceanSurface = Color(0xFF0E1A2B)    // Caribbean twilight surface
val PristineOceanCard = Color(0xFF1D324F)       // Deep tide reef card


// Dynamic Generic Dark Palette for backward compatibility
val DarkPrimary = Color(0xFF3E8EFF)
val DarkSecondary = Color(0xFF00D1FF)
val DarkTertiary = Color(0xFF8B5CF6)

// Dynamic Generic Light Palette for backward compatibility
val LightPrimary = Color(0xFF0F2D59)
val LightSecondary = Color(0xFF26A69A)
val LightTertiary = Color(0xFF10B981)

