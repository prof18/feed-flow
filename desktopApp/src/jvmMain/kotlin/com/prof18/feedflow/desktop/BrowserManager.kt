package com.prof18.feedflow.desktop

import com.prof18.feedflow.core.model.ArticleOpenMode
import com.prof18.feedflow.shared.data.SettingsRepository

internal class BrowserManager(
    private val settingsRepository: SettingsRepository,
) {
    fun getArticleOpenMode(): ArticleOpenMode =
        settingsRepository.getArticleOpenMode()
}
