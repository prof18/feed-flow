package com.prof18.feedflow.shared.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.jthemedetecor.OsThemeDetector
import com.prof18.feedflow.shared.ui.style.DarkColorScheme
import com.prof18.feedflow.shared.ui.style.LightColorScheme
import org.jetbrains.skiko.SystemTheme
import org.jetbrains.skiko.currentSystemTheme

@Composable
fun FeedFlowTheme(
    darkTheme: Boolean = rememberDesktopDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}

@Composable
fun rememberDesktopDarkTheme(): Boolean {
    var darkTheme by remember {
        mutableStateOf(currentSystemTheme == SystemTheme.DARK)
    }

    DisposableEffect(Unit) {
        val darkThemeListener: (Boolean) -> Unit = {
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
