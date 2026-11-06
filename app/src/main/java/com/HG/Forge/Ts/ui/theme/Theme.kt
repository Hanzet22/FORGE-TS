package com.HG.Forge.Ts.ui.theme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
private val ForgeColors = lightColorScheme(
 primary=Color(0xFF245A91),onPrimary=Color.White,primaryContainer=Color(0xFFDCEAF8),
 onPrimaryContainer=Color(0xFF173653),secondary=Color(0xFF173653),
 background=Color(0xFFF7F9FC),surface=Color.White
)
@Composable
fun ForgeTheme(content:@Composable ()->Unit) { MaterialTheme(colorScheme=ForgeColors,content=content) }
