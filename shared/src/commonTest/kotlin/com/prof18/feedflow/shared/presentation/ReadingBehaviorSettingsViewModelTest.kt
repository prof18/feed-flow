package com.prof18.feedflow.shared.presentation

import app.cash.turbine.test
import com.prof18.feedflow.shared.test.KoinTestBase
import kotlinx.coroutines.test.runTest
import org.koin.test.inject
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReadingBehaviorSettingsViewModelTest : KoinTestBase() {

    private val viewModel: ReadingBehaviorSettingsViewModel by inject()

    @Test
    fun `state is loaded from settings repository on init`() = runTest {
        viewModel.state.test {
            val initialState = awaitItem()
            // Default values from SettingsRepository
            assertTrue(initialState.isReaderModeEnabled)
            assertFalse(initialState.isSaveReaderModeContentEnabled)
            assertFalse(initialState.isPrefetchArticleContentEnabled)
            assertTrue(initialState.isMarkReadWhenScrollingEnabled)
            assertFalse(initialState.isShowReadItemsEnabled)
            assertFalse(initialState.isHideReadItemsEnabled)
        }
    }

    @Test
    fun `updateReaderMode updates state`() = runTest {
        viewModel.state.test {
            awaitItem()

            viewModel.updateReaderMode(false)
            assertFalse(awaitItem().isReaderModeEnabled)

            viewModel.updateReaderMode(true)
            assertTrue(awaitItem().isReaderModeEnabled)
        }
    }

    @Test
    fun `updateSaveReaderModeContent updates state`() = runTest {
        viewModel.state.test {
            awaitItem()

            viewModel.updateSaveReaderModeContent(true)
            assertTrue(awaitItem().isSaveReaderModeContentEnabled)

            viewModel.updateSaveReaderModeContent(false)
            assertFalse(awaitItem().isSaveReaderModeContentEnabled)
        }
    }

    @Test
    fun `updatePrefetchArticleContent updates state`() = runTest {
        viewModel.state.test {
            awaitItem()

            viewModel.updatePrefetchArticleContent(true)
            assertTrue(awaitItem().isPrefetchArticleContentEnabled)

            viewModel.updatePrefetchArticleContent(false)
            assertFalse(awaitItem().isPrefetchArticleContentEnabled)
        }
    }

    @Test
    fun `updateMarkReadWhenScrolling updates state`() = runTest {
        viewModel.state.test {
            awaitItem()

            viewModel.updateMarkReadWhenScrolling(false)
            assertFalse(awaitItem().isMarkReadWhenScrollingEnabled)

            viewModel.updateMarkReadWhenScrolling(true)
            assertTrue(awaitItem().isMarkReadWhenScrollingEnabled)
        }
    }

    @Test
    fun `updateShowReadItemsOnTimeline updates state`() = runTest {
        viewModel.state.test {
            awaitItem()

            viewModel.updateShowReadItemsOnTimeline(true)
            assertTrue(awaitItem().isShowReadItemsEnabled)

            viewModel.updateShowReadItemsOnTimeline(false)
            assertFalse(awaitItem().isShowReadItemsEnabled)
        }
    }

    @Test
    fun `updateHideReadItems updates state`() = runTest {
        viewModel.state.test {
            awaitItem()

            viewModel.updateHideReadItems(true)
            assertTrue(awaitItem().isHideReadItemsEnabled)

            viewModel.updateHideReadItems(false)
            assertFalse(awaitItem().isHideReadItemsEnabled)
        }
    }
}
