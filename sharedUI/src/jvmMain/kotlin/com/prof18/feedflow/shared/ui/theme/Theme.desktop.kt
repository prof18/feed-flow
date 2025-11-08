package com.prof18.feedflow.shared.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.jthemedetecor.OsThemeDetector
import com.prof18.feedflow.shared.ui.style.DarkColorScheme
import com.prof18.feedflow.shared.ui.style.LightColorScheme
import com.prof18.feedflow.shared.ui.utils.LocalReduceMotion
import java.util.function.Consumer

@Composable
fun FeedFlowTheme(
    darkTheme: Boolean = rememberDesktopDarkTheme(),
    reduceMotion: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    CompositionLocalProvider(LocalReduceMotion provides reduceMotion) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content,
        )
    }
}

@Composable
actual fun FeedFlowThemePreview(
    content: @Composable () -> Unit,
) {
    FeedFlowTheme(
        darkTheme = rememberDesktopDarkTheme(),
        content = content,
    )
}

@Composable
fun rememberDesktopDarkTheme(): Boolean {
    var darkTheme by remember {
        mutableStateOf(OsThemeDetector.getDetector().isDark)
    }

    DisposableEffect(Unit) {
        val darkThemeListener = Consumer<Boolean> {
            darkTheme = it
        }

        val detector = OsThemeDetector.getDetector().apply {
            registerListener(darkThemeListener)
        }

        onDispose {
            detector.removeListener(darkThemeListener)
        }
    }

    return darkTheme
}
