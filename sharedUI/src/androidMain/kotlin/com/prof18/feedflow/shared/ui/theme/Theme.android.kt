package com.prof18.feedflow.shared.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.prof18.feedflow.shared.ui.style.DarkColorScheme
import com.prof18.feedflow.shared.ui.style.LightColorScheme
import com.prof18.feedflow.shared.ui.style.OledColorScheme

@Composable
fun FeedFlowTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    useOledTheme: Boolean = false,
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        useOledTheme -> OledColorScheme
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        content = content,
    )
}

@Composable
actual fun FeedFlowThemePreview(
    content: @Composable () -> Unit,
) {
    FeedFlowTheme(
        content = content,
    )
}
