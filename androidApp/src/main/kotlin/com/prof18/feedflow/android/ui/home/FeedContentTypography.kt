package com.prof18.feedflow.android.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.font.FontFamily
import com.prof18.feedflow.android.ui.readerfont.rememberComposeFontFamily
import com.prof18.feedflow.core.model.ReaderFontFamily
import com.prof18.feedflow.core.model.ReaderModeDefaults

@Immutable
data class FeedContentTypography(
    val titleFontFamily: FontFamily? = null,
    val bodyFontFamily: FontFamily? = null,
)

val LocalFeedContentTypography = staticCompositionLocalOf { FeedContentTypography() }

@Composable
fun rememberFeedContentTypography(
    headlineFont: ReaderFontFamily = ReaderModeDefaults.HEADLINE_FONT,
    bodyFont: ReaderFontFamily = ReaderModeDefaults.BODY_FONT,
): FeedContentTypography {
    val titleFontFamily = rememberComposeFontFamily(headlineFont)
    val bodyFontFamily = rememberComposeFontFamily(bodyFont)
    return remember(titleFontFamily, bodyFontFamily) {
        FeedContentTypography(
            titleFontFamily = titleFontFamily,
            bodyFontFamily = bodyFontFamily,
        )
    }
}
