package com.HG.Forge.Ts.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.HG.Forge.Ts.R
import com.HG.Forge.Ts.data.preferences.ForgeThemeMode

val ForgeSans = FontFamily(
    Font(R.font.forge_archivo_regular, FontWeight.Normal),
    Font(R.font.forge_archivo_semibold, FontWeight.SemiBold),
    Font(R.font.forge_archivo_bold, FontWeight.Bold)
)
val ForgeMono = FontFamily(Font(R.font.forge_azeret_mono, FontWeight.Normal))
val ForgeReading = ForgeSans
val ForgeDisplay = FontFamily(Font(R.font.forge_anton, FontWeight.Normal))
val ForgeScript = FontFamily(Font(R.font.kaushan_script_regular, FontWeight.Normal))

private val ForgeLightColors = lightColorScheme(
    primary = Color(0xFF245A91), onPrimary = Color.White,
    primaryContainer = Color(0xFFDCEAF8), onPrimaryContainer = Color(0xFF173653),
    secondary = Color(0xFF315A7C), onSecondary = Color.White,
    background = Color(0xFFF7F9FC), surface = Color.White,
    surfaceVariant = Color(0xFFE8EDF3), onSurface = Color(0xFF151B22),
    onSurfaceVariant = Color(0xFF485563), outline = Color(0xFF788795)
)
private val ForgeBlackColors = darkColorScheme(
    primary = Color(0xFF9CCBFF), onPrimary = Color(0xFF003258),
    primaryContainer = Color(0xFF174B76), onPrimaryContainer = Color(0xFFD3E8FF),
    secondary = Color(0xFFB6C9DD), background = Color(0xFF101316),
    surface = Color(0xFF171B20), surfaceVariant = Color(0xFF293039),
    onSurface = Color(0xFFE5EAF0), onSurfaceVariant = Color(0xFFC0CAD4),
    outline = Color(0xFF84919E)
)
private val ForgeAmoledColors = darkColorScheme(
    primary = Color(0xFF9CCBFF), onPrimary = Color(0xFF003258),
    primaryContainer = Color(0xFF174B76), onPrimaryContainer = Color(0xFFD3E8FF),
    secondary = Color(0xFFB6C9DD), background = Color.Black, surface = Color.Black,
    surfaceDim = Color.Black, surfaceBright = Color(0xFF24282D),
    surfaceContainerLowest = Color.Black, surfaceContainerLow = Color(0xFF080B0F),
    surfaceContainer = Color(0xFF101419), surfaceContainerHigh = Color(0xFF171C22),
    surfaceContainerHighest = Color(0xFF20262D), surfaceVariant = Color(0xFF20262D),
    onSurface = Color(0xFFF0F3F7), onSurfaceVariant = Color(0xFFC1CAD4),
    outline = Color(0xFF788795)
)
private val ForgeSuperBlackColors = darkColorScheme(
    primary = Color(0xFFB6D8FF), onPrimary = Color(0xFF002D4D),
    primaryContainer = Color(0xFF103C63), onPrimaryContainer = Color(0xFFD8EBFF),
    secondary = Color(0xFFC0D4E8), background = Color.Black, surface = Color.Black,
    surfaceDim = Color.Black, surfaceBright = Color(0xFF101010),
    surfaceContainerLowest = Color.Black, surfaceContainerLow = Color.Black,
    surfaceContainer = Color.Black, surfaceContainerHigh = Color.Black,
    surfaceContainerHighest = Color(0xFF050505), surfaceVariant = Color(0xFF090909),
    onSurface = Color.White, onSurfaceVariant = Color(0xFFD0D0D0),
    outline = Color(0xFF777777)
)
private val ForgeTypography = Typography(
    displayLarge = TextStyle(fontFamily = ForgeDisplay, fontSize = 56.sp, lineHeight = 60.sp),
    displayMedium = TextStyle(fontFamily = ForgeDisplay, fontSize = 44.sp, lineHeight = 48.sp),
    headlineLarge = TextStyle(fontFamily = ForgeDisplay, fontSize = 34.sp, lineHeight = 40.sp),
    headlineMedium = TextStyle(fontFamily = ForgeDisplay, fontSize = 28.sp, lineHeight = 34.sp),
    headlineSmall = TextStyle(fontFamily = ForgeSans, fontWeight = FontWeight.Bold, fontSize = 24.sp),
    titleLarge = TextStyle(fontFamily = ForgeSans, fontWeight = FontWeight.Bold, fontSize = 22.sp),
    titleMedium = TextStyle(fontFamily = ForgeSans, fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    titleSmall = TextStyle(fontFamily = ForgeSans, fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    bodyLarge = TextStyle(fontFamily = ForgeSans, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = ForgeSans, fontSize = 14.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontFamily = ForgeSans, fontSize = 12.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = ForgeSans, fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    labelMedium = TextStyle(fontFamily = ForgeMono, fontSize = 11.sp),
    labelSmall = TextStyle(fontFamily = ForgeMono, fontSize = 10.sp)
)
@Composable
fun ForgeTheme(themeMode: ForgeThemeMode = ForgeThemeMode.DYNAMIC_COLOR, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val systemDark = isSystemInDarkTheme()
    val supportsDynamic = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val scheme = when (themeMode) {
        ForgeThemeMode.WHITE -> ForgeLightColors
        ForgeThemeMode.BLACK -> ForgeBlackColors
        ForgeThemeMode.AMOLED_BLACK -> ForgeAmoledColors
        ForgeThemeMode.SUPER_BLACK -> ForgeSuperBlackColors
        ForgeThemeMode.DYNAMIC_COLOR -> when {
            supportsDynamic && systemDark -> dynamicDarkColorScheme(context)
            supportsDynamic -> dynamicLightColorScheme(context)
            systemDark -> ForgeBlackColors
            else -> ForgeLightColors
        }
    }
    MaterialTheme(colorScheme = scheme, typography = ForgeTypography, content = content)
}
