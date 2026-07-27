package com.prof18.feedflow.shared.data

import com.prof18.feedflow.core.model.ArticleOpenMode
import com.prof18.feedflow.core.model.appDefaultArticleOpenMode
import com.prof18.feedflow.shared.test.KoinTestBase
import kotlinx.coroutines.test.runTest
import org.koin.test.inject
import kotlin.test.Test
import kotlin.test.assertEquals

class SettingsRepositoryArticleOpenModeTest : KoinTestBase() {

    private val repository: SettingsRepository by inject()

    @Test
    fun `article open mode defaults to the app default when nothing is stored`() = runTest {
        assertEquals(appDefaultArticleOpenMode, repository.getArticleOpenMode())
        assertEquals(ArticleOpenMode.FULL_ARTICLE, appDefaultArticleOpenMode)
    }

    @Test
    fun `setArticleOpenMode stores concrete modes as they are`() = runTest {
        for (mode in listOf(
            ArticleOpenMode.FULL_ARTICLE,
            ArticleOpenMode.FEED_CONTENT,
            ArticleOpenMode.INTERNAL_BROWSER,
            ArticleOpenMode.PREFERRED_BROWSER,
        )) {
            repository.setArticleOpenMode(mode)
            assertEquals(mode, repository.getArticleOpenMode())
        }
    }

    @Test
    fun `DEFAULT is stored as the app default because it only means follow-global on a feed`() = runTest {
        repository.setArticleOpenMode(ArticleOpenMode.PREFERRED_BROWSER)

        repository.setArticleOpenMode(ArticleOpenMode.DEFAULT)

        assertEquals(appDefaultArticleOpenMode, repository.getArticleOpenMode())
    }
}
