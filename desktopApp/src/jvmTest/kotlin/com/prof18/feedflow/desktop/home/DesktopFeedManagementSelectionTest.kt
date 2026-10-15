package com.prof18.feedflow.desktop.home

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.MouseButton
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import com.prof18.feedflow.core.model.CategoryNameValidationResult
import com.prof18.feedflow.core.model.DrawerItem
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedFontSizes
import com.prof18.feedflow.core.model.FeedLayout
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.FeedSourceCategory
import com.prof18.feedflow.core.model.NavDrawerState
import com.prof18.feedflow.core.model.NoFeedSourcesStatus
import com.prof18.feedflow.core.model.SwipeActionType
import com.prof18.feedflow.core.model.SwipeActions
import com.prof18.feedflow.core.utils.getDesktopOS
import com.prof18.feedflow.core.utils.isMacOs
import com.prof18.feedflow.desktop.home.drawer.DesktopDrawer
import com.prof18.feedflow.desktop.testing.DesktopLibraryFixture
import com.prof18.feedflow.desktop.ui.FeedManagementActions
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.desktop.ui.LocalReduceMotion
import com.prof18.feedflow.desktop.ui.components.FeedFlowTheme
import com.prof18.feedflow.i18n.EnFeedFlowStrings
import com.prof18.feedflow.shared.presentation.model.HomeDisplayState
import com.prof18.feedflow.shared.presentation.model.NextFeedDisplayState
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DesktopFeedManagementSelectionTest {
    @get:Rule
    val rule = createComposeRule()

    private val sources = DesktopLibraryFixture.load().drawerState().feedSourcesByCategory.values.flatten()
        .filterIsInstance<DrawerItem.DrawerFeedSource>()
        .take(3).mapIndexed { index, source -> source.copy(feedSource = source.feedSource.copy(title = "Feed $index")) }

    @Test
    fun `modified click and shift range select without activating then clear selection`() {
        var activations = 0
        setHost { activations++ }
        modifiedClick("Feed 0", toggleKey)
        modifiedClick("Feed 2", Key.ShiftLeft)
        rule.onNodeWithText("3 selected").assertExists()
        rule.onNodeWithText("Feed 1").assertIsSelected()
        assertEquals(0, activations)
        rule.onNodeWithText("Clear selection").performMouseInput { click() }
        rule.onNodeWithText("0 selected").assertDoesNotExist()
    }

    @Test
    fun `context on a selected row preserves batch but an unselected row replaces it`() {
        var activations = 0
        setHost { activations++ }
        modifiedClick("Feed 0", toggleKey)
        modifiedClick("Feed 1", toggleKey)
        rule.onNodeWithText("Feed 1").performMouseInput { click(button = MouseButton.Secondary) }
        rule.onNodeWithText("2 selected").assertExists()
        rule.onNodeWithText(EnFeedFlowStrings.openFeedSettings).assertDoesNotExist()
        rule.onNodeWithText("Feed 2").performKeyInput { pressKey(Key.Escape) }
        rule.onNodeWithText("Feed 2").performMouseInput { click(button = MouseButton.Secondary) }
        rule.onNodeWithText("1 selected").assertExists()
        rule.onNodeWithText(EnFeedFlowStrings.openFeedSettings).assertExists()
        assertEquals(0, activations)
    }

    @Test
    fun `modified Enter and shift Enter share pointer selection semantics`() {
        var activations = 0
        setHost { activations++ }
        val first = rule.onNodeWithText("Feed 0")
        first.requestFocus()
        first.performKeyInput {
            keyDown(toggleKey)
            pressKey(Key.Enter)
            keyUp(toggleKey)
        }
        val last = rule.onNodeWithText("Feed 2")
        last.requestFocus()
        last.performKeyInput {
            keyDown(Key.ShiftLeft)
            pressKey(Key.Enter)
            keyUp(Key.ShiftLeft)
        }
        rule.onNodeWithText("3 selected").assertExists()
        assertEquals(0, activations)
    }

    @Test
    fun `keyboard context targets the focused source without opening articles`() {
        var activations = 0
        setHost { activations++ }
        val row = rule.onNodeWithText("Feed 1")
        row.requestFocus()
        row.performKeyInput {
            keyDown(Key.ShiftLeft)
            pressKey(Key.F10)
            keyUp(Key.ShiftLeft)
        }
        rule.onNodeWithText(EnFeedFlowStrings.openFeedSettings).assertExists()
        rule.onNodeWithText("1 selected").assertExists()
        assertEquals(0, activations)
    }

    @Test
    fun `batch delete requires confirmation and targets each selected identity once`() {
        val deleted = mutableListOf<List<FeedSource>>()
        setHost(onDelete = { deleted.add(it) }, onActivate = {})
        modifiedClick("Feed 0", toggleKey)
        modifiedClick("Feed 1", toggleKey)
        rule.onNodeWithText(EnFeedFlowStrings.desktopDeleteSelectedFeeds).performMouseInput { click() }
        rule.onNodeWithText(EnFeedFlowStrings.cancelButton).performSemanticsAction(SemanticsActions.OnClick) { it() }
        assertEquals(emptyList(), deleted)
        rule.onNodeWithText(EnFeedFlowStrings.desktopDeleteSelectedFeeds).performMouseInput { click() }
        rule.onNodeWithText(EnFeedFlowStrings.deleteFeed).performSemanticsAction(SemanticsActions.OnClick) { it() }
        rule.runOnIdle {
            val expectedIds = sources.take(2).map { it.feedSource.id }.toSet()
            val deletedIds = deleted.map { feeds -> feeds.map { it.id }.toSet() }
            assertEquals(listOf(expectedIds), deletedIds)
            assertEquals(2, deleted.single().size)
        }
    }

    @Test
    fun `move confirmation uses current feed metadata while cancel makes no change`() {
        var drawer by mutableStateOf(NavDrawerState(feedSourcesWithoutCategory = sources.toImmutableList()))
        val moved = mutableListOf<List<FeedSource>>()
        setHost(drawerState = { drawer }, onMove = { feeds, _ -> moved.add(feeds) }, onActivate = {})
        modifiedClick("Feed 0", toggleKey)
        rule.onNodeWithText(EnFeedFlowStrings.desktopMoveSelectedFeeds).performMouseInput { click() }
        rule.onNodeWithText(EnFeedFlowStrings.cancelButton).performSemanticsAction(SemanticsActions.OnClick) { it() }
        assertEquals(emptyList(), moved)
        rule.onNodeWithText(EnFeedFlowStrings.desktopMoveSelectedFeeds).performMouseInput { click() }
        rule.runOnIdle {
            val updated = sources.first().copy(
                feedSource = sources.first().feedSource.copy(title = "Renamed", isPinned = true),
            )
            drawer = drawer.copy(feedSourcesWithoutCategory = (listOf(updated) + sources.drop(1)).toImmutableList())
        }
        rule.onNodeWithText(EnFeedFlowStrings.confirmButton).performSemanticsAction(SemanticsActions.OnClick) { it() }
        rule.runOnIdle {
            assertEquals(1, moved.size)
            assertEquals("Renamed", moved.single().single().title)
            assertEquals(true, moved.single().single().isPinned)
        }
    }

    @Test
    fun `management arrows focus visible rows without activation and Enter activates once`() {
        val activations = mutableListOf<FeedFilter>()
        setHost(onActivate = activations::add)
        rule.onNodeWithText("Feed 0").requestFocus()
        rule.onNodeWithText("Feed 0").performKeyInput { pressKey(Key.DirectionDown) }
        rule.onNodeWithText("Feed 1").assertIsFocused()
        rule.onNodeWithText("Feed 1").performKeyInput { pressKey(Key.DirectionUp) }
        rule.onNodeWithText("Feed 0").assertIsFocused()
        assertEquals(emptyList(), activations)
        rule.onNodeWithText("Feed 0").performKeyInput { pressKey(Key.Enter) }
        assertEquals(listOf<FeedFilter>(FeedFilter.Source(sources[0].feedSource)), activations)
    }

    @Test
    fun `management Left collapses parent and Right expands without activating articles`() {
        var activations = 0
        setHost { activations++ }
        rule.onNodeWithText("Feed 0").requestFocus()
        rule.onNodeWithText("Feed 0").performKeyInput { pressKey(Key.DirectionLeft) }
        rule.onNodeWithText(categoryTitle).assertIsFocused()
        rule.onNodeWithText("Feed 0").assertDoesNotExist()
        rule.onNodeWithText(categoryTitle).performKeyInput { pressKey(Key.DirectionRight) }
        rule.onNodeWithText("Feed 0").assertExists()
        rule.onNodeWithText(categoryTitle).assertIsFocused()
        assertEquals(0, activations)
    }

    @Test
    fun `category primary click activates once and secondary click opens context only`() {
        var activations = 0
        setHost { activations++ }
        rule.onNodeWithText(categoryTitle).performMouseInput { click() }
        assertEquals(1, activations)
        rule.onNodeWithText(categoryTitle).performMouseInput { click(button = MouseButton.Secondary) }
        rule.onNodeWithText(EnFeedFlowStrings.deleteAllFeedsInCategory).assertExists()
        assertEquals(1, activations)
    }

    @Test
    fun `pointer reorder dispatches current section IDs exactly once`() {
        val reordered = mutableListOf<List<FeedSource>>()
        setHost(onReorder = reordered::add, onActivate = {})
        val origin = rule.onNodeWithText("Feed 0").fetchSemanticsNode().boundsInRoot
        val target = rule.onNodeWithText("Feed 2").fetchSemanticsNode().boundsInRoot
        rule.onNodeWithText("Feed 0").performMouseInput {
            moveTo(center)
            press()
            moveTo(Offset(center.x, target.bottom - origin.top - 1f))
            release()
        }
        rule.runOnIdle {
            assertEquals(
                listOf(sources[1].feedSource.id, sources[2].feedSource.id, sources[0].feedSource.id),
                reordered.single().map { it.id },
            )
        }
    }

    @Test
    fun `Escape cancels active pointer reorder without changing sources`() {
        val reordered = mutableListOf<List<FeedSource>>()
        setHost(onReorder = reordered::add, onActivate = {})
        rule.onNodeWithText("Feed 0").requestFocus()
        rule.onNodeWithText("Feed 0").performMouseInput {
            moveTo(center)
            press()
            moveTo(Offset(center.x, center.y + DRAG_REORDER_DISTANCE))
        }
        assertEquals(2, rule.onAllNodesWithText("Feed 0").fetchSemanticsNodes().size)
        rule.onAllNodesWithText("Feed 0")[0].performKeyInput { pressKey(Key.Escape) }
        assertEquals(1, rule.onAllNodesWithText("Feed 0").fetchSemanticsNodes().size)
        rule.onNodeWithText("Feed 0").performMouseInput { release() }
        assertEquals(emptyList(), reordered)
    }

    @Test
    fun `drag move resolves current metadata at drop and only moves eligible IDs`() {
        val destination = requireNotNull(sources[0].feedSource.category).copy(id = "destination", title = "Destination")
        var drawer by mutableStateOf(
            NavDrawerState(
                feedSourcesWithoutCategory = sources.toImmutableList(),
                categories = persistentListOf(DrawerItem.DrawerCategory(destination, unreadCount = 0)),
            ),
        )
        val moved = mutableListOf<List<FeedSource>>()
        setHost(drawerState = { drawer }, onMove = { feeds, category ->
            assertEquals(destination.id, category?.id)
            moved.add(feeds)
        }, onActivate = {})
        val target = rule.onNodeWithText(destination.title).fetchSemanticsNode().boundsInRoot
        val origin = rule.onNodeWithText("Feed 0").fetchSemanticsNode().boundsInRoot
        rule.onNodeWithText("Feed 0").performMouseInput {
            moveTo(center)
            press()
            moveTo(Offset(center.x, target.center.y - origin.top))
        }
        rule.runOnIdle {
            drawer = drawer.copy(
                feedSourcesWithoutCategory = sources.mapIndexed { index, item ->
                    if (index == 0) {
                        item.copy(feedSource = item.feedSource.copy(title = "Current title", isPinned = true))
                    } else {
                        item
                    }
                }.toImmutableList(),
            )
        }
        rule.onAllNodesWithText("Current title")[0].performMouseInput { release() }
        rule.runOnIdle {
            assertEquals(1, moved.size)
            assertEquals(sources[0].feedSource.id, moved.single().single().id)
            assertEquals("Current title", moved.single().single().title)
            assertEquals(true, moved.single().single().isPinned)
        }
    }

    @Test
    fun `management arrows scroll to the final source without activating it`() {
        val library = DesktopLibraryFixture.load()
        var activations = 0
        setHost(drawerState = { library.drawerState() }) { activations++ }
        rule.onNodeWithText(EnFeedFlowStrings.drawerTitleTimeline).requestFocus()
        val tree = projectDesktopFeedTree(library.drawerState())
        val rowCount = PRIMARY_ROWS + tree.pinned.size + tree.sections.size +
            tree.sections.sumOf { it.visibleFeeds.size }
        repeat(rowCount - 1) {
            rule.onRoot().performKeyInput {
                pressKey(Key.DirectionDown)
            }
            rule.waitForIdle()
        }
        val last = tree.sections.last().visibleFeeds.last().feedSource
        rule.onNodeWithText(last.title).assertIsFocused()
        assertEquals(0, activations)
    }

    @Test
    fun `Shift arrows extend management selection without changing the filter`() {
        var activations = 0
        setHost { activations++ }
        modifiedClick("Feed 0", toggleKey)
        rule.onNodeWithText("Feed 0").requestFocus()
        rule.onNodeWithText("Feed 0").performKeyInput {
            keyDown(Key.ShiftLeft)
            pressKey(Key.DirectionDown)
            keyUp(Key.ShiftLeft)
        }
        rule.onNodeWithText("2 selected").assertExists()
        rule.onNodeWithText("Feed 1").assertIsFocused()
        assertEquals(0, activations)
    }

    @Test
    fun `disclosure focus navigates from its category rather than the previously focused source`() {
        setHost(onActivate = {})
        rule.onNodeWithText("Feed 0").requestFocus()
        rule.onNodeWithText("Feed 0").performKeyInput {
            keyDown(Key.ShiftLeft)
            pressKey(Key.Tab)
            keyUp(Key.ShiftLeft)
        }
        val label = EnFeedFlowStrings.desktopCollapseFeedCategory(requireNotNull(sources[0].feedSource.category).title)
        rule.onNodeWithContentDescription(label).assertIsFocused()
        rule.onNodeWithContentDescription(label).performKeyInput { pressKey(Key.DirectionDown) }
        rule.onNodeWithText("Feed 0").assertIsFocused()
    }

    @Test
    fun `category pointer reorder submits each category identity once`() {
        val first = requireNotNull(sources[0].feedSource.category)
        val second = first.copy(id = "second-category", title = "Second category", position = first.position + 1)
        val reordered = mutableListOf<List<DrawerItem.DrawerFeedSource.FeedSourceCategoryWrapper>>()
        setHost(
            drawerState = {
                NavDrawerState(
                    categories = persistentListOf(
                        DrawerItem.DrawerCategory(first, unreadCount = 0),
                        DrawerItem.DrawerCategory(second, unreadCount = 0),
                    ),
                )
            },
            onReorderCategories = reordered::add,
            onActivate = {},
        )
        val target = rule.onNodeWithText(second.title).fetchSemanticsNode().boundsInRoot
        val origin = rule.onNodeWithText(first.title).fetchSemanticsNode().boundsInRoot
        rule.onNodeWithText(first.title).performMouseInput {
            moveTo(center)
            press()
            moveTo(Offset(center.x, target.bottom - origin.top + 1f))
            release()
        }
        rule.runOnIdle {
            assertEquals(listOf(second.id, first.id), reordered.single().map { it.feedSourceCategory?.id })
        }
    }

    @Test
    fun `mixed selection drag evaluates current category eligibility for every captured ID`() {
        val originalCategory = requireNotNull(sources[0].feedSource.category)
        val targetCategory = originalCategory.copy(id = "destination", title = "Destination")
        val originalSources = sources.mapIndexed { index, item ->
            if (index == 1) item.copy(feedSource = item.feedSource.copy(category = targetCategory)) else item
        }
        var drawer by mutableStateOf(
            NavDrawerState(
                feedSourcesWithoutCategory = originalSources.toImmutableList(),
                categories = persistentListOf(DrawerItem.DrawerCategory(targetCategory, unreadCount = 0)),
            ),
        )
        val moved = mutableListOf<List<FeedSource>>()
        setHost(drawerState = { drawer }, onMove = { feeds, _ -> moved.add(feeds) }, onActivate = {})
        modifiedClick("Feed 0", toggleKey)
        modifiedClick("Feed 1", toggleKey)
        val target = rule.onNodeWithText(targetCategory.title).fetchSemanticsNode().boundsInRoot
        val origin = rule.onNodeWithText("Feed 0").fetchSemanticsNode().boundsInRoot
        rule.onNodeWithText("Feed 0").performMouseInput {
            moveTo(center)
            press()
            moveTo(Offset(center.x, target.center.y - origin.top))
        }
        rule.runOnIdle {
            drawer = drawer.copy(
                feedSourcesWithoutCategory = originalSources.mapIndexed { index, item ->
                    if (index == 1) item.copy(feedSource = item.feedSource.copy(category = originalCategory)) else item
                }.toImmutableList(),
            )
        }
        rule.onAllNodesWithText("Feed 0")[0].performMouseInput { release() }
        rule.runOnIdle {
            assertEquals(sources.take(2).map { it.feedSource.id }.toSet(), moved.single().map { it.id }.toSet())
            assertEquals(2, moved.single().size)
        }
    }

    @Test
    fun `holding a source at the lower edge scrolls the tree and Escape cancels the drop`() {
        val library = DesktopLibraryFixture.load()
        val moved = mutableListOf<List<FeedSource>>()
        setHost(
            drawerState = { library.drawerState() },
            onMove = { feeds, _ -> moved.add(feeds) },
            onActivate = {},
        )
        val first = library.drawerState().pinnedFeedSources.filterIsInstance<DrawerItem.DrawerFeedSource>().first()
        val label = first.feedSource.title
        rule.onNodeWithText(label).requestFocus()
        val origin = rule.onNodeWithText(label).fetchSemanticsNode().boundsInRoot
        val bottom = rule.onRoot().fetchSemanticsNode().boundsInRoot.bottom
        rule.onNodeWithText(label).performMouseInput {
            moveTo(center)
            press()
            moveTo(Offset(x = center.x, y = bottom - origin.top - 24f))
        }
        val last = projectDesktopFeedTree(library.drawerState()).sections.last().visibleFeeds.last().feedSource.title
        rule.waitUntil(timeoutMillis = 5000) { rule.onAllNodesWithText(last).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText(last).assertIsDisplayed()
        rule.onRoot().performKeyInput { pressKey(Key.Escape) }
        rule.onRoot().performMouseInput { release() }
        assertEquals(emptyList(), moved)
    }

    private fun modifiedClick(title: String, modifierKey: Key) {
        val row = rule.onNodeWithText(title)
        row.performKeyInput { keyDown(modifierKey) }
        try {
            row.performMouseInput { click() }
        } finally {
            row.performKeyInput { keyUp(modifierKey) }
        }
    }

    @Test
    fun `reduced motion settles category feed reordering without animated placement`() {
        var drawer by mutableStateOf(NavDrawerState(feedSourcesWithoutCategory = sources.toImmutableList()))
        setHost(drawerState = { drawer }, reduceMotion = true, onActivate = {})
        assertImmediatePlacement("Feed 0") {
            drawer = drawer.copy(feedSourcesWithoutCategory = sources.reversed().toImmutableList())
        }
    }

    @Test
    fun `reduced motion settles pinned feed reordering without animated placement`() {
        val pinned = sources.map { it.copy(feedSource = it.feedSource.copy(isPinned = true)) }.toImmutableList()
        var drawer by mutableStateOf(NavDrawerState(pinnedFeedSources = pinned))
        setHost(drawerState = { drawer }, reduceMotion = true, onActivate = {})
        assertImmediatePlacement("Feed 0") {
            drawer = drawer.copy(pinnedFeedSources = pinned.reversed().toImmutableList())
        }
    }

    @Test
    fun `reduced motion settles category header reordering without animated placement`() {
        val first = FeedSourceCategory(id = "first", title = "First group", position = 0)
        val second = FeedSourceCategory(id = "second", title = "Second group", position = 1)
        var drawer by mutableStateOf(
            NavDrawerState(
                categories = persistentListOf(
                    DrawerItem.DrawerCategory(first, 0),
                    DrawerItem.DrawerCategory(second, 0),
                ),
            ),
        )
        setHost(drawerState = { drawer }, reduceMotion = true, onActivate = {})
        assertImmediatePlacement(first.title) {
            drawer = drawer.copy(
                categories = persistentListOf(
                    DrawerItem.DrawerCategory(first.copy(position = 1), 0),
                    DrawerItem.DrawerCategory(second.copy(position = 0), 0),
                ),
            )
        }
    }

    private fun assertImmediatePlacement(title: String, reorder: () -> Unit) {
        rule.waitForIdle()
        val original = rule.onNodeWithText(title).fetchSemanticsNode().boundsInRoot.top
        rule.mainClock.autoAdvance = false
        rule.runOnIdle(reorder)
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeByFrame()
        val immediate = rule.onNodeWithText(title).fetchSemanticsNode().boundsInRoot.top
        rule.mainClock.advanceTimeBy(milliseconds = 2_000)
        val settled = rule.onNodeWithText(title).fetchSemanticsNode().boundsInRoot.top
        assertTrue(original != settled, "The fixture must actually move the target row")
        assertEquals(settled, immediate, "Reduce Motion must place the row without intermediate movement")
    }

    private fun setHost(
        drawerState: () -> NavDrawerState = { NavDrawerState(feedSourcesWithoutCategory = sources.toImmutableList()) },
        onMove: (List<FeedSource>, FeedSourceCategory?) -> Unit = { _, _ -> },
        onDelete: (List<FeedSource>) -> Unit = {},
        onReorder: (List<FeedSource>) -> Unit = {},
        onReorderCategories: (List<DrawerItem.DrawerFeedSource.FeedSourceCategoryWrapper>) -> Unit = {},
        reduceMotion: Boolean = false,
        onActivate: (FeedFilter) -> Unit,
    ) {
        rule.setContent {
            CompositionLocalProvider(
                LocalFeedFlowStrings provides EnFeedFlowStrings,
                LocalReduceMotion provides reduceMotion,
            ) {
                FeedFlowTheme(darkTheme = false, useOledTheme = false) {
                    DesktopDrawer(
                        displayState = HomeDisplayState(
                            feedItems = persistentListOf(),
                            navDrawerState = drawerState(),
                            unReadCount = 0, isUnreadCountHidden = false, feedUpdateStatus = NoFeedSourcesStatus,
                            feedFontSizes = FeedFontSizes(), currentFeedFilter = FeedFilter.Timeline,
                            nextFeedDisplayState = NextFeedDisplayState.NextFeedDisplayDisabledState,
                            swipeActions = SwipeActions(SwipeActionType.NONE, SwipeActionType.NONE),
                            feedLayout = FeedLayout.LIST, isGridLayoutEnabled = false,
                        ),
                        feedManagementActions = FeedManagementActions(
                            onAddFeedClick = {}, onFeedFilterSelected = {}, onEditFeedClick = {},
                            onDeleteFeedSourceClick = {}, onPinFeedClick = {}, onEditCategoryClick = { _, _ -> },
                            validateCategoryName = { _, _ -> CategoryNameValidationResult.VALID },
                            onDeleteCategoryClick = {}, onOpenWebsite = {},
                            onMoveFeedSourcesToCategory = onMove,
                            onReorderFeedSources = onReorder,
                            onReorderCategories = onReorderCategories,
                            onDeleteAllFeedsInCategoryClick = onDelete,
                        ),
                        onFeedFilterSelected = onActivate,
                    )
                }
            }
        }
    }

    private val categoryTitle = requireNotNull(sources[0].feedSource.category).title

    private companion object {
        const val DRAG_REORDER_DISTANCE = 110f
        const val PRIMARY_ROWS = 3
    }

    private val toggleKey = if (getDesktopOS().isMacOs()) Key.MetaLeft else Key.CtrlLeft
}
