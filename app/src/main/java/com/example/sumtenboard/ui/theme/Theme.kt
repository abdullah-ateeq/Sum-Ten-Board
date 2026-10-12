package com.example.sumtenboard.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.sumtenboard.data.ThemeMode

private val LightColors = lightColorScheme(
    primary = TealLight,
    onPrimary = Color.White,
    primaryContainer = TealContainerLight,
    onPrimaryContainer = OnTealContainerLight,
    secondary = BlueLight,
    onSecondary = Color.White,
    secondaryContainer = BlueContainerLight,
    onSecondaryContainer = OnBlueContainerLight,
    tertiary = ClayLight,
    onTertiary = Color.White,
    tertiaryContainer = ClayContainerLight,
    onTertiaryContainer = Charcoal,
    error = ErrorLight,
    onError = Color.White,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
    background = WarmWhite,
    onBackground = Charcoal,
    surface = WarmWhite,
    onSurface = Charcoal,
    surfaceVariant = SoftGray,
    onSurfaceVariant = CharcoalMuted,
    surfaceContainerLowest = PaperWhite,
    surfaceContainerLow = SoftGrayLow,
    surfaceContainer = SoftGray,
    surfaceContainerHigh = SoftGrayHigh,
    surfaceContainerHighest = SoftGrayHighest,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight
)

private val DarkColors = darkColorScheme(
    primary = TealDark,
    onPrimary = NearBlack,
    primaryContainer = TealContainerDark,
    onPrimaryContainer = OnTealContainerDark,
    secondary = BlueDark,
    onSecondary = NearBlack,
    secondaryContainer = BlueContainerDark,
    onSecondaryContainer = OnBlueContainerDark,
    tertiary = ClayDark,
    onTertiary = NearBlack,
    tertiaryContainer = ClayContainerDark,
    onTertiaryContainer = SoftWhite,
    error = ErrorDark,
    onError = NearBlack,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    background = NearBlack,
    onBackground = SoftWhite,
    surface = NearBlack,
    onSurface = SoftWhite,
    surfaceVariant = GraphiteHigh,
    onSurfaceVariant = SoftWhiteMuted,
    surfaceContainerLowest = NearBlack,
    surfaceContainerLow = GraphiteLow,
    surfaceContainer = Graphite,
    surfaceContainerHigh = GraphiteHigh,
    surfaceContainerHighest = GraphiteHighest,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark
)

@Composable
fun SumTenBoardTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
