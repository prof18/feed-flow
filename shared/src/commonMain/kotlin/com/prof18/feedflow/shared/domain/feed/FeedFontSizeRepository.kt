package com.prof18.feedflow.shared.domain.feed

import com.prof18.feedflow.core.model.FeedFontSizes
import com.prof18.feedflow.core.model.plus
import com.prof18.feedflow.shared.domain.settings.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class FeedFontSizeRepository(
    private val settingsRepository: SettingsRepository,
) {

    private val feedFontSizeMutableState: MutableStateFlow<FeedFontSizes> = MutableStateFlow(
        getDefaultFontSize(),
    )
    val feedFontSizeState = feedFontSizeMutableState.asStateFlow()

    private fun getDefaultFontSize(): FeedFontSizes {
        val scaleFactor = settingsRepository.getFeedListFontScaleFactor()
        return FeedFontSizes() + scaleFactor
    }
}
