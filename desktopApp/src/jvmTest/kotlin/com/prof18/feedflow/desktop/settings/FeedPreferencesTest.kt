package com.prof18.feedflow.desktop.settings

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.sp
import com.prof18.feedflow.core.model.FEED_TITLE_FONT_SIZE
import com.prof18.feedflow.core.model.FeedFontSizes
import com.prof18.feedflow.core.model.FeedListSettingsState
import com.prof18.feedflow.core.model.FeedOrder
import com.prof18.feedflow.core.model.plus
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.desktop.ui.components.FeedFlowTheme
import com.prof18.feedflow.i18n.EnFeedFlowStrings
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

class FeedPreferencesTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `named font controls update the live preview and respect scale limits`() {
        val scale = mutableStateOf(0)
        compose.setContent {
            CompositionLocalProvider(LocalFeedFlowStrings provides EnFeedFlowStrings) {
                FeedFlowTheme(darkTheme = false, useOledTheme = false) {
                    FeedListPane(
                        fontSizesState = FeedFontSizes() + scale.value,
                        settingsState = FeedListSettingsState(),
                        feedOrder = FeedOrder.NEWEST_FIRST,
                        onFontScaleUpdate = { scale.value = it },
                        onFeedLayoutUpdate = {},
                        onGridLayoutEnabledUpdate = {},
                        onHideDescriptionUpdate = {},
                        onHideImagesUpdate = {},
                        onHideDateUpdate = {},
                        onRemoveTitleFromDescUpdate = {},
                        onDateFormatUpdate = {},
                        onTimeFormatUpdate = {},
                        onSwipeActionUpdate = { _, _ -> },
                        onFeedOrderSelected = {},
                        onHideUnreadDotUpdate = {},
                        onHideFeedSourceUpdate = {},
                        onDescriptionLineLimitUpdate = {},
                    )
                }
            }
        }
        val increase = compose.onNodeWithContentDescription(EnFeedFlowStrings.increaseFontSizeButtonContentDescription)
        val decrease = compose.onNodeWithContentDescription(EnFeedFlowStrings.decreaseFontSizeButtonContentDescription)
        compose.onNodeWithContentDescription(EnFeedFlowStrings.settingsFeedListScaleTitle).assertExists()
        increase.performSemanticsAction(SemanticsActions.OnClick) { it() }
        compose.runOnIdle { assertEquals(1, scale.value) }
        val layouts = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText(EnFeedFlowStrings.settingsFontScaleTitleExample, useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertEquals((FEED_TITLE_FONT_SIZE + 1).sp, layouts.single().layoutInput.style.fontSize)
        decrease.performSemanticsAction(SemanticsActions.OnClick) { it() }
        compose.runOnIdle {
            assertEquals(0, scale.value)
            scale.value = MAX_FONT_SCALE
        }
        increase.assertIsNotEnabled()
        increase.performSemanticsAction(SemanticsActions.OnClick) { it() }
        compose.runOnIdle {
            assertEquals(MAX_FONT_SCALE, scale.value)
            scale.value = MIN_FONT_SCALE
        }
        decrease.assertIsNotEnabled()
        decrease.performSemanticsAction(SemanticsActions.OnClick) { it() }
        compose.runOnIdle { assertEquals(MIN_FONT_SCALE, scale.value) }
    }

    private companion object {
        const val MAX_FONT_SCALE = 16
        const val MIN_FONT_SCALE = -4
    }
}
