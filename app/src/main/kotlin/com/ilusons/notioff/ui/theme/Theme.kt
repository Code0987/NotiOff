package com.ilusons.notioff.ui.theme

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

// Brand-aligned with legacy amber/orange palette (K15: follow system light/dark).
private val NotiOffAmber = Color(0xFFFFC107)
private val NotiOffAmberDark = Color(0xFFFFA000)
private val NotiOffAccent = Color(0xFFFF5722)

private val LightColors = lightColorScheme(
    primary = NotiOffAmberDark,
    onPrimary = Color.Black,
    secondary = NotiOffAccent,
    onSecondary = Color.White,
    tertiary = NotiOffAmber,
)

private val DarkColors = darkColorScheme(
    primary = NotiOffAmber,
    onPrimary = Color.Black,
    secondary = NotiOffAccent,
    onSecondary = Color.White,
    tertiary = NotiOffAmberDark,
)

@Composable
fun NotiOffTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
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
        content = content,
    )
}
