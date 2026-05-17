package com.lurenjia534.quotahub.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.lurenjia534.quotahub.data.preferences.ThemeColorSource
import com.lurenjia534.quotahub.data.preferences.ThemePalette

private val QuotaHubShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun QuotaHubTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    themeColorSource: ThemeColorSource = ThemeColorSource.System,
    themePalette: ThemePalette = ThemePalette.QuotaHub,
    dynamicColor: Boolean = themeColorSource == ThemeColorSource.System,
    content: @Composable () -> Unit
) {
    val useSystemDynamicColor = dynamicColor &&
        themeColorSource == ThemeColorSource.System &&
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val colorScheme = when {
        useSystemDynamicColor -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        else -> quotaHubColorScheme(
            palette = themePalette,
            darkTheme = darkTheme
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        motionScheme = MotionScheme.expressive(),
        typography = Typography,
        shapes = QuotaHubShapes,
        content = content
    )
}
