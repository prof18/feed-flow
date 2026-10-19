package com.prof18.feedflow.desktop.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.key
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.LayoutDirection
import cafe.adriel.lyricist.LanguageTag
import com.prof18.feedflow.desktop.ui.components.FeedFlowTheme
import com.prof18.feedflow.desktop.ui.components.desktopPanelBackground

@Composable
internal fun DesktopUiEnvironment(
    darkTheme: Boolean,
    useOledTheme: Boolean = false,
    reduceMotionEnabled: Boolean = false,
    forceEnglishEnabled: Boolean = false,
    currentLanguageTag: LanguageTag = Locale.current.toLanguageTag(),
    content: @Composable () -> Unit,
) {
    val languageTag = if (forceEnglishEnabled) "en" else currentLanguageTag
    val lyricist = key(languageTag) { rememberFeedFlowStrings(currentLanguageTag = languageTag) }
    val deviceLayoutDirection = LocalLayoutDirection.current
    val layoutDirection = if (forceEnglishEnabled) LayoutDirection.Ltr else deviceLayoutDirection
    FeedFlowTheme(darkTheme, useOledTheme) {
        ProvideFeedFlowStrings(lyricist) {
            CompositionLocalProvider(
                LocalReduceMotion provides reduceMotionEnabled,
                LocalLayoutDirection provides layoutDirection,
                content = content,
            )
        }
    }
}

internal fun desktopWindowBackgroundColor(darkTheme: Boolean, useOledTheme: Boolean): java.awt.Color =
    java.awt.Color(desktopPanelBackground(darkTheme, useOledTheme).toArgb())

@Composable
@ReadOnlyComposable
internal fun desktopMainWindowTitle(): String = LocalFeedFlowStrings.current.appName
