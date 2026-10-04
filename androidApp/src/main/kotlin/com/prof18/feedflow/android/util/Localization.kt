package com.prof18.feedflow.android.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.text.intl.Locale
import cafe.adriel.lyricist.Lyricist
import com.prof18.feedflow.i18n.FeedFlowStrings
import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.ui.utils.rememberFeedFlowStrings

@Composable
internal fun rememberAndroidFeedFlowStrings(settingsRepository: SettingsRepository): Lyricist<FeedFlowStrings> {
    val forceEnglishEnabled by settingsRepository.forceEnglishEnabledFlow.collectAsState()
    val languageTag = if (forceEnglishEnabled) "en" else Locale.current.toLanguageTag()
    return key(languageTag) {
        rememberFeedFlowStrings(currentLanguageTag = languageTag)
    }
}
