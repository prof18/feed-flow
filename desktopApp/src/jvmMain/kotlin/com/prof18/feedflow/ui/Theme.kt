package com.prof18.feedflow.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.jthemedetecor.OsThemeDetector
import org.jetbrains.skiko.SystemTheme
import org.jetbrains.skiko.currentSystemTheme

private val LightColorScheme = lightColorScheme(
    primary = LightAppColors.primary,
//    primaryVariant = LightAppColors.green2,
//    secondary = LightAppColors.yellow1,
//    secondaryVariant = LightAppColors.yellow2,

    background = LightAppColors.background,
//    surface = LightAppColors.gray4,
    error = LightAppColors.red1,

//    onPrimary = LightAppColors.lightGrey,
//    onSecondary = DarkAppColors.gray4,
//    onBackground = DarkAppColors.gray4,
//    onSurface = DarkAppColors.gray4,
//    onError = DarkAppColors.gray4,
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkAppColors.primary,
//    primaryVariant = DarkAppColors.green2,
//    secondary = DarkAppColors.yellow1,
//    secondaryVariant = DarkAppColors.yellow2,

//    background = backgroundColorDark,
//    surface = primaryBlueDark, // It's for example for the bottom bar
    error = DarkAppColors.red1,

//    onPrimary = LightAppColors.gray4,
//    onSecondary = LightAppColors.gray4,
//    onBackground = LightAppColors.gray4,
//    onSurface = LightAppColors.gray4,
//    onError = LightAppColors.gray4,
)

@Composable
internal fun FeedFlowTheme(
    darkTheme: Boolean = rememberDesktopDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    println("is dark theme? $darkTheme")

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
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