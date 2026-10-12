package com.example.sumtenboard.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val Base = Typography()

val Typography = Typography(
    displayMedium = Base.displayMedium.copy(fontWeight = FontWeight.Black, letterSpacing = 2.sp),
    displaySmall = Base.displaySmall.copy(fontWeight = FontWeight.Black, letterSpacing = 1.sp),
    headlineMedium = Base.headlineMedium.copy(fontWeight = FontWeight.Bold),
    headlineSmall = Base.headlineSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
    titleLarge = Base.titleLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
    titleMedium = Base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    labelLarge = Base.labelLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
    labelMedium = Base.labelMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp),
    labelSmall = Base.labelSmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
)

/** Style for the numbers on board tiles; size is set per tile from its measured size. */
val TileNumberStyle = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.Bold
)
