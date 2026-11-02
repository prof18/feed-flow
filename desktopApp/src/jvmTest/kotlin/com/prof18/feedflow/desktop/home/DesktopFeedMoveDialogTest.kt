@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package com.prof18.feedflow.desktop.home

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import com.prof18.feedflow.core.model.ArticleOpenMode
import com.prof18.feedflow.core.model.CategoryName
import com.prof18.feedflow.core.model.CategoryNameValidationResult
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.FeedSourceCategory
import com.prof18.feedflow.desktop.home.drawer.DesktopFeedMoveDialog
import com.prof18.feedflow.desktop.testing.FeedFormE2eIds
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.desktop.ui.components.FeedFlowTheme
import com.prof18.feedflow.i18n.EnFeedFlowStrings
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DesktopFeedMoveDialogTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `a category created from the picker becomes the move destination`() {
        val created = mutableListOf<CategoryName>()
        var categories: ImmutableList<FeedSourceCategory> by mutableStateOf(persistentListOf())
        var moved: Pair<List<FeedSource>, FeedSourceCategory?>? = null
        val source = feedSource()

        rule.setContent {
            CompositionLocalProvider(LocalFeedFlowStrings provides EnFeedFlowStrings) {
                FeedFlowTheme(darkTheme = false, useOledTheme = false) {
                    DesktopFeedMoveDialog(
                        categories = categories,
                        feedSources = persistentListOf(source),
                        onMove = { sources, category -> moved = sources to category },
                        onCreateCategory = { name ->
                            created += name
                            categories = (categories + FeedSourceCategory(id = "created-1", title = name.name))
                                .toImmutableList()
                        },
                        validateCategoryName = { _, _ -> CategoryNameValidationResult.VALID },
                        onDismiss = { },
                    )
                }
            }
        }

        rule.waitUntilExactlyOneExists(hasText(EnFeedFlowStrings.cancelButton), timeoutMillis = 5_000)
        openDestinationPicker()
        rule.waitUntilExactlyOneExists(hasText(EnFeedFlowStrings.desktopNewCategoryMenu), timeoutMillis = 5_000)
        rule.onNodeWithText(EnFeedFlowStrings.desktopNewCategoryMenu)
            .performSemanticsAction(SemanticsActions.OnClick) { it() }

        rule.waitUntilExactlyOneExists(hasText(EnFeedFlowStrings.newCategoryHint), timeoutMillis = 5_000)
        rule.onNodeWithTag(FeedFormE2eIds.CATEGORY_ADD_INPUT).performTextInput("Reading")
        rule.onNodeWithTag(FeedFormE2eIds.CATEGORY_ADD_CONFIRM)
            .performSemanticsAction(SemanticsActions.OnClick) { it() }
        rule.waitUntilDoesNotExist(hasText(EnFeedFlowStrings.newCategoryHint), timeoutMillis = 5_000)

        rule.runOnIdle { assertEquals(listOf(CategoryName("Reading")), created) }
        // The picker shows the new category without a second trip through the menu.
        rule.waitUntilExactlyOneExists(hasText("Reading"), timeoutMillis = 5_000)

        rule.onNodeWithText(EnFeedFlowStrings.confirmButton)
            .performSemanticsAction(SemanticsActions.OnClick) { it() }
        rule.runOnIdle {
            assertEquals(listOf(source), moved?.first)
            assertEquals("created-1", moved?.second?.id)
        }
    }

    @Test
    fun `picking no category moves a categorised feed out of its category`() {
        val category = FeedSourceCategory(id = "category-1", title = "News")
        var moved: Pair<List<FeedSource>, FeedSourceCategory?>? = null
        val source = feedSource(category = category)

        rule.setContent {
            CompositionLocalProvider(LocalFeedFlowStrings provides EnFeedFlowStrings) {
                FeedFlowTheme(darkTheme = false, useOledTheme = false) {
                    DesktopFeedMoveDialog(
                        categories = persistentListOf(category),
                        feedSources = persistentListOf(source),
                        onMove = { sources, destination -> moved = sources to destination },
                        onCreateCategory = { },
                        validateCategoryName = { _, _ -> CategoryNameValidationResult.VALID },
                        onDismiss = { },
                    )
                }
            }
        }

        rule.waitUntilExactlyOneExists(hasText(EnFeedFlowStrings.cancelButton), timeoutMillis = 5_000)
        rule.onNodeWithText(EnFeedFlowStrings.confirmButton)
            .performSemanticsAction(SemanticsActions.OnClick) { it() }

        rule.runOnIdle {
            assertEquals(listOf(source), moved?.first)
            assertNull(moved?.second)
        }
    }

    private fun openDestinationPicker() {
        rule.onNode(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.DropdownList))
            .performSemanticsAction(SemanticsActions.OnClick) { it() }
    }

    private fun feedSource(category: FeedSourceCategory? = null) = FeedSource(
        id = "source-1",
        url = "https://example.com/feed.xml",
        title = "Example feed",
        category = category,
        logoUrl = null,
        websiteUrl = "https://example.com",
        lastSyncTimestamp = null,
        fetchFailed = false,
        articleOpenMode = ArticleOpenMode.DEFAULT,
        isHiddenFromTimeline = false,
        isPinned = false,
        isNotificationEnabled = false,
        isHideImagesEnabled = false,
    )
}
