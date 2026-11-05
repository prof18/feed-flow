package com.prof18.feedflow.desktop.settings

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isNotSelected
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.core.model.SyncAccounts
import com.prof18.feedflow.desktop.settings.search.SETTINGS_SEARCH_CLEAR_TAG
import com.prof18.feedflow.desktop.settings.search.SETTINGS_SEARCH_FIELD_TAG
import com.prof18.feedflow.desktop.settings.search.SETTINGS_SEARCH_RESULTS_TAG
import com.prof18.feedflow.desktop.settings.search.SettingsSearchEntry
import com.prof18.feedflow.desktop.settings.search.settingsSearchIndex
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.desktop.ui.components.FeedFlowTheme
import com.prof18.feedflow.i18n.EnFeedFlowStrings
import kotlinx.coroutines.runBlocking
import org.jetbrains.jewel.ui.component.OutlinedButton
import org.jetbrains.jewel.ui.component.Text
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// Kept apart from DesktopSettingsWindowTest: a native Compose window running on the EDT sends snapshot
// notifications that invalidate this rule's layout nodes off the test thread, which Compose rejects.
class DesktopSettingsContentTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `keyboard arrows move category focus and Enter activates the category`() {
        compose.setContent {
            SettingsContent(DesktopSettingsCategory.GENERAL)
        }

        val categoryList = compose.onNodeWithTag(SETTINGS_CATEGORIES_TAG)
        categoryList.requestFocus()
        categoryList.performKeyInput {
            pressKey(Key.DirectionDown)
            pressKey(Key.Enter)
        }

        compose.onNodeWithText(EnFeedFlowStrings.settingsAccounts).assertIsSelected()
        compose.onNodeWithTag(DETAIL_TAG).assertTextEquals(DesktopSettingsCategory.ACCOUNTS.name)
    }

    @Test
    fun `external initial category change updates selection and details`() {
        val initialCategory = mutableStateOf(DesktopSettingsCategory.GENERAL)
        compose.setContent {
            SettingsContent(initialCategory.value)
        }

        compose.runOnIdle { initialCategory.value = DesktopSettingsCategory.FEED_LIST }

        compose.onNodeWithText(EnFeedFlowStrings.settingsFeedListTitle).assertIsSelected()
        compose.onNodeWithTag(DETAIL_TAG).assertTextEquals(DesktopSettingsCategory.FEED_LIST.name)
    }

    @Test
    fun `filters has a dedicated settings navigation page`() {
        compose.setContent { SettingsContent(DesktopSettingsCategory.GENERAL) }
        compose.onNodeWithText("Filters").performClick()
        compose.onNodeWithTag(DETAIL_TAG).assertTextEquals("FILTERS")
    }

    @Test
    fun `category selection remains selected as focus leaves and returns to navigation`() {
        compose.setContent {
            SettingsContent(DesktopSettingsCategory.GENERAL)
        }
        val categoryList = compose.onNodeWithTag(SETTINGS_CATEGORIES_TAG)
        categoryList.requestFocus()
        categoryList.performKeyInput {
            repeat(times = 3) { pressKey(Key.DirectionDown) }
            pressKey(Key.Enter)
        }
        val readingCategory = compose.onNodeWithText(EnFeedFlowStrings.settingsReadingBehavior)
        readingCategory.assertIsSelected()
        val detailAction = compose.onNodeWithTag(DETAIL_ACTION_TAG)
        detailAction.requestFocus()
        detailAction.assertIsFocused()
        compose.onNodeWithText(EnFeedFlowStrings.settingsReadingBehavior).assertIsSelected()

        categoryList.requestFocus()

        categoryList.assertIsFocused()
        compose.onNodeWithText(EnFeedFlowStrings.settingsReadingBehavior).assertIsSelected()
        compose.onNodeWithTag(DETAIL_TAG).assertTextEquals(DesktopSettingsCategory.READING.name)
    }

    @Test
    fun `scroll position is restored when returning to a settings category`() {
        val feedListScrollState = AtomicReference<ScrollState?>()
        compose.setContent {
            SettingsContent(
                initialCategory = DesktopSettingsCategory.GENERAL,
                paneContent = { category ->
                    if (category == DesktopSettingsCategory.FEED_LIST) {
                        val scrollState = rememberScrollState()
                        feedListScrollState.set(scrollState)
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(scrollState)
                                .testTag(SCROLLING_PANE_TAG),
                        ) {
                            repeat(times = 40) { Text("Feed list setting $it", modifier = Modifier.padding(8.dp)) }
                        }
                    } else {
                        Text(category.name, modifier = Modifier.testTag(DETAIL_TAG))
                    }
                },
            )
        }

        val categoryList = compose.onNodeWithTag(SETTINGS_CATEGORIES_TAG)
        categoryList.requestFocus()
        categoryList.performKeyInput {
            repeat(2) { pressKey(Key.DirectionDown) }
            pressKey(Key.Enter)
        }
        compose.runOnIdle {
            val state = requireNotNull(feedListScrollState.get())
            assertTrue(state.maxValue > SAVED_SCROLL_POSITION)
            runBlocking { state.scrollTo(value = SAVED_SCROLL_POSITION) }
        }
        val savedPosition = requireNotNull(feedListScrollState.get()).value

        categoryList.requestFocus()
        categoryList.performKeyInput {
            repeat(times = 4) { pressKey(Key.DirectionDown) }
            pressKey(Key.Enter)
        }
        compose.onNodeWithTag(DETAIL_TAG).assertTextEquals(DesktopSettingsCategory.ABOUT.name)

        categoryList.requestFocus()
        categoryList.performKeyInput {
            repeat(times = 4) { pressKey(Key.DirectionUp) }
            pressKey(Key.Enter)
        }

        compose.runOnIdle {
            assertEquals(savedPosition, requireNotNull(feedListScrollState.get()).value)
        }
    }

    @Test
    fun `typing a query replaces categories with results and choosing one opens its page`() {
        compose.setContent { SettingsContent(DesktopSettingsCategory.FEED_LIST) }

        val field = compose.onNodeWithTag(SETTINGS_SEARCH_FIELD_TAG)
        field.performTextInput("unread count")
        compose.onNodeWithTag(SETTINGS_CATEGORIES_TAG).assertDoesNotExist()
        compose.onNodeWithTag(SETTINGS_SEARCH_RESULTS_TAG).assertExists()
        compose.onNodeWithText(EnFeedFlowStrings.settingsHideUnreadCount).performClick()

        compose.onNodeWithTag(DETAIL_TAG).assertTextEquals(DesktopSettingsCategory.GENERAL.name)
        field.assertTextContains("unread count")
    }

    @Test
    fun `Enter opens the first result`() {
        compose.setContent { SettingsContent(DesktopSettingsCategory.GENERAL) }

        val field = compose.onNodeWithTag(SETTINGS_SEARCH_FIELD_TAG)
        field.performTextInput("swipe")
        field.performKeyInput { pressKey(Key.Enter) }

        compose.onNodeWithTag(DETAIL_TAG).assertTextEquals(DesktopSettingsCategory.FEED_LIST.name)
    }

    @Test
    fun `Down then Enter opens the second result`() {
        compose.setContent { SettingsContent(DesktopSettingsCategory.ABOUT) }

        val field = compose.onNodeWithTag(SETTINGS_SEARCH_FIELD_TAG)
        field.performTextInput("unread")
        field.performKeyInput {
            pressKey(Key.DirectionDown)
            pressKey(Key.Enter)
        }

        compose.onNodeWithTag(DETAIL_TAG).assertTextEquals(DesktopSettingsCategory.FEED_LIST.name)
    }

    @Test
    fun `keyboard-active result is exposed as selected`() {
        compose.setContent { SettingsContent(DesktopSettingsCategory.ABOUT) }

        val field = compose.onNodeWithTag(SETTINGS_SEARCH_FIELD_TAG)
        field.performTextInput("unread")
        compose.onNode(hasText(EnFeedFlowStrings.settingsHideUnreadCount) and isSelected()).assertExists()
        field.performKeyInput { pressKey(Key.DirectionDown) }

        compose.onNode(hasText(EnFeedFlowStrings.settingsHideUnreadDot) and isSelected()).assertExists()
        compose.onNode(hasText(EnFeedFlowStrings.settingsHideUnreadCount) and isNotSelected()).assertExists()
    }

    @Test
    fun `Escape clears the query and restores categories`() {
        compose.setContent { SettingsContent(DesktopSettingsCategory.GENERAL) }

        val field = compose.onNodeWithTag(SETTINGS_SEARCH_FIELD_TAG)
        field.performTextInput("theme")
        field.performKeyInput { pressKey(Key.Escape) }

        compose.onNodeWithTag(SETTINGS_CATEGORIES_TAG).assertExists()
        assertEquals("", field.fetchSemanticsNode().config[SemanticsProperties.EditableText].text)
    }

    @Test
    fun `clear button empties the query and restores categories`() {
        compose.setContent { SettingsContent(DesktopSettingsCategory.GENERAL) }

        val field = compose.onNodeWithTag(SETTINGS_SEARCH_FIELD_TAG)
        compose.onNodeWithTag(SETTINGS_SEARCH_CLEAR_TAG).assertDoesNotExist()
        field.performTextInput("about")
        val clear = compose.onNodeWithTag(SETTINGS_SEARCH_CLEAR_TAG)
        // Mouse-only, so Enter typed in the field never lands on it.
        clear.requestFocus()
        clear.assertIsNotFocused()
        clear.performClick()

        compose.onNodeWithTag(SETTINGS_CATEGORIES_TAG).assertExists()
        compose.onNodeWithTag(SETTINGS_SEARCH_CLEAR_TAG).assertDoesNotExist()
        assertEquals("", field.fetchSemanticsNode().config[SemanticsProperties.EditableText].text)
    }

    @Test
    fun `no results shows the empty message`() {
        compose.setContent { SettingsContent(DesktopSettingsCategory.GENERAL) }

        compose.onNodeWithTag(SETTINGS_SEARCH_FIELD_TAG).performTextInput("zzzz")

        compose.onNodeWithText(EnFeedFlowStrings.desktopSettingsSearchNoResults).assertExists()
    }

    @Test
    fun `shrinking the index resets the active result`() {
        val entries = mutableStateOf(
            listOf(
                SettingsSearchEntry(DesktopSettingsCategory.GENERAL, "Alpha one", "General"),
                SettingsSearchEntry(DesktopSettingsCategory.READING, "Alpha two", "Reading"),
            ),
        )
        compose.setContent { SettingsContent(DesktopSettingsCategory.ABOUT, searchEntries = entries.value) }

        val field = compose.onNodeWithTag(SETTINGS_SEARCH_FIELD_TAG)
        field.performTextInput("alpha")
        field.performKeyInput { pressKey(Key.DirectionDown) }
        compose.runOnIdle { entries.value = entries.value.take(1) }
        field.performKeyInput { pressKey(Key.Enter) }

        compose.onNodeWithTag(DETAIL_TAG).assertTextEquals(DesktopSettingsCategory.GENERAL.name)
    }

    @Test
    fun `keyboard selection keeps the active result visible`() {
        compose.setContent {
            Box(Modifier.requiredSize(width = 900.dp, height = 300.dp)) {
                SettingsContent(
                    DesktopSettingsCategory.GENERAL,
                    searchEntries = List(30) {
                        SettingsSearchEntry(DesktopSettingsCategory.ABOUT, "Item $it", "About")
                    },
                )
            }
        }

        val field = compose.onNodeWithTag(SETTINGS_SEARCH_FIELD_TAG)
        field.performTextInput("item")
        field.performKeyInput { repeat(times = 25) { pressKey(Key.DirectionDown) } }

        compose.waitUntil(timeoutMillis = 2_000) {
            runCatching { compose.onNodeWithText("Item 25").assertIsDisplayed() }.isSuccess
        }
    }

    @Composable
    private fun SettingsContent(
        initialCategory: DesktopSettingsCategory,
        onCategorySelected: (DesktopSettingsCategory) -> Unit = {},
        searchEntries: List<SettingsSearchEntry> = settingsSearchIndex(
            strings = EnFeedFlowStrings,
            supportedAccounts = listOf(SyncAccounts.DROPBOX),
            isCrashReportingAvailable = false,
            isGridToggleVisible = true,
            isFaqEnabled = false,
        ),
        paneContent: @Composable (DesktopSettingsCategory) -> Unit = { category ->
            Column {
                Text(category.name, modifier = Modifier.testTag(DETAIL_TAG))
                OutlinedButton(
                    onClick = { onCategorySelected(category) },
                    modifier = Modifier.testTag(DETAIL_ACTION_TAG),
                ) {
                    Text("Detail action")
                }
            }
        },
    ) {
        CompositionLocalProvider(LocalFeedFlowStrings provides EnFeedFlowStrings) {
            FeedFlowTheme(darkTheme = false, useOledTheme = false) {
                SettingsContent(
                    initialCategory = initialCategory,
                    modifier = Modifier.fillMaxSize(),
                    searchEntries = searchEntries,
                    content = paneContent,
                )
            }
        }
    }

    private companion object {
        const val DETAIL_TAG = "settings-detail"
        const val DETAIL_ACTION_TAG = "settings-detail-action"
        const val SCROLLING_PANE_TAG = "settings-scroll-pane"
        const val SAVED_SCROLL_POSITION = 80
    }
}
