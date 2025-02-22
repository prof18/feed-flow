package com.prof18.feedflow.shared.domain.feed

import com.prof18.feedflow.core.model.FeedFontSizes
import com.prof18.feedflow.core.model.plus
import com.prof18.feedflow.shared.data.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

internal class FeedFontSizeRepository(
    private val settingsRepository: SettingsRepository,
) {

    private val feedFontSizeMutableState: MutableStateFlow<FeedFontSizes> = MutableStateFlow(
        getDefaultFontSize(),
    )
    val feedFontSizeState = feedFontSizeMutableState.asStateFlow()

    fun updateFontScale(value: Int) {
        settingsRepository.setFeedListFontScaleFactor(value)
        feedFontSizeMutableState.update {
            FeedFontSizes() + value
        }
    }

    private fun getDefaultFontSize(): FeedFontSizes {
        val scaleFactor = settingsRepository.getFeedListFontScaleFactor()
        return FeedFontSizes() + scaleFactor
    }
}
