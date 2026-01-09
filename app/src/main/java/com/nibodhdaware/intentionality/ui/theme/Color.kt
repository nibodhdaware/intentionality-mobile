package com.nibodhdaware.intentionality.ui.theme

import androidx.compose.ui.graphics.Color

// Modern Dark Theme - Clean and Cohesive
// Based on Material 3 guidelines with a neutral dark palette

// Primary colors - Soft teal/cyan accent
val Primary = Color(0xFF80CBC4)           // Soft teal - main accent
val OnPrimary = Color(0xFF003733)         // Dark teal for text on primary
val PrimaryContainer = Color(0xFF004D46)  // Darker teal for containers
val OnPrimaryContainer = Color(0xFFA7F3EC) // Light teal for text on container

// Secondary colors - Muted blue-gray
val Secondary = Color(0xFFB0BEC5)          // Blue-gray accent
val OnSecondary = Color(0xFF1C313A)        // Dark blue-gray
val SecondaryContainer = Color(0xFF334955) // Container background
val OnSecondaryContainer = Color(0xFFCCE3EC) // Light for text

// Background and Surface - True dark with subtle elevation
val Background = Color(0xFF121212)         // True dark background
val OnBackground = Color(0xFFE3E3E3)       // Off-white text
val Surface = Color(0xFF1E1E1E)            // Slightly elevated surface
val OnSurface = Color(0xFFE3E3E3)          // Off-white text
val SurfaceVariant = Color(0xFF2C2C2C)     // Cards and elevated elements
val OnSurfaceVariant = Color(0xFFCACACA)   // Muted text

// Error colors
val Error = Color(0xFFCF6679)              // Soft red
val OnError = Color(0xFF1E1E1E)

// Outline
val Outline = Color(0xFF444444)            // Subtle borders

// For backwards compatibility with existing code
val BackgroundDark = Background
val PrimaryDark = Primary
val AccentDark = Primary
val SurfaceDark = Surface
val OnBackgroundDark = OnBackground
val OnPrimaryDark = OnPrimary
val OnSurfaceDark = OnSurface

// Light Theme colors
val BackgroundLight = Color(0xFFFFFBFE)
val PrimaryLight = Color(0xFF006A62)
val AccentLight = Color(0xFF006A62)
val SurfaceLight = Color(0xFFFFFBFE)
val OnBackgroundLight = Color(0xFF1C1B1F)
val OnPrimaryLight = Color(0xFFFFFFFF)
val OnSurfaceLight = Color(0xFF1C1B1F)