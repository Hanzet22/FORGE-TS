package com.HG.Forge.Ts.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.HG.Forge.Ts.R

val ForgeSans = FontFamily(
    Font(R.font.dejavu_sans, FontWeight.Normal, FontStyle.Normal),
    Font(R.font.dejavu_sans_bold, FontWeight.Bold, FontStyle.Normal),
    Font(R.font.dejavu_sans_oblique, FontWeight.Normal, FontStyle.Italic),
    Font(R.font.dejavu_sans_bold_oblique, FontWeight.Bold, FontStyle.Italic)
)

val ForgeScript = FontFamily(Font(R.font.kaushan_script_regular, FontWeight.Normal))

private val ForgeColors = lightColorScheme(
    primary = Color(0xFF245A91),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCEAF8),
    onPrimaryContainer = Color(0xFF173653),
    secondary = Color(0xFF173653),
    background = Color(0xFFF7F9FC),
    surface = Color.White
)

private val ForgeTypography = Typography(
    bodyLarge = TextStyle(fontFamily = ForgeSans, fontSize = 16.sp),
    bodyMedium = TextStyle(fontFamily = ForgeSans, fontSize = 14.sp),
    bodySmall = TextStyle(fontFamily = ForgeSans, fontSize = 12.sp),
    titleLarge = TextStyle(fontFamily = ForgeSans, fontWeight = FontWeight.Bold, fontSize = 22.sp),
    titleMedium = TextStyle(fontFamily = ForgeSans, fontWeight = FontWeight.Bold, fontSize = 16.sp),
    titleSmall = TextStyle(fontFamily = ForgeSans, fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    headlineSmall = TextStyle(fontFamily = ForgeSans, fontWeight = FontWeight.Bold, fontSize = 24.sp),
    labelLarge = TextStyle(fontFamily = ForgeSans, fontWeight = FontWeight.Medium, fontSize = 14.sp),
    labelMedium = TextStyle(fontFamily = ForgeSans, fontWeight = FontWeight.Medium, fontSize = 12.sp),
    labelSmall = TextStyle(fontFamily = ForgeSans, fontWeight = FontWeight.Medium, fontSize = 11.sp)
)

@Composable
fun ForgeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ForgeColors,
        typography = ForgeTypography,
        content = content
    )
}
