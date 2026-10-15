package com.prof18.feedflow.desktop.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.core.model.CategoryNameValidationResult
import com.prof18.feedflow.core.model.DrawerItem
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedFontSizes
import com.prof18.feedflow.core.model.FeedLayout
import com.prof18.feedflow.core.model.NoFeedSourcesStatus
import com.prof18.feedflow.core.model.SwipeActionType
import com.prof18.feedflow.core.model.SwipeActions
import com.prof18.feedflow.core.model.ThemeMode
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
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.junit.Rule
import org.junit.Test
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DesktopFeedManagementIconContrastTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `add feed icon has visible contrast in every theme`() {
        val theme = mutableStateOf(ThemeMode.LIGHT)
        setHost(theme)
        assertIconInEveryTheme(theme, EnFeedFlowStrings.addFeed)
    }

    @Test
    fun `category disclosure icon has visible contrast in every theme`() {
        val theme = mutableStateOf(ThemeMode.LIGHT)
        val fixture = setHost(theme)
        val description = EnFeedFlowStrings.desktopCollapseFeedCategory(fixture.category.title)
        assertIconInEveryTheme(theme, description)
    }

    @Test
    fun `no-logo source icon has visible contrast in every theme and row remains actionable`() {
        val theme = mutableStateOf(ThemeMode.LIGHT)
        var selectedFilter: FeedFilter? = null
        val fixture = setHost(theme) { selectedFilter = it }
        listOf(ThemeMode.LIGHT, ThemeMode.DARK, ThemeMode.OLED).forEach { mode ->
            rule.runOnIdle { theme.value = mode }
            val drawerBounds = rule.onNodeWithTag(DRAWER_TAG).fetchSemanticsNode().boundsInRoot
            val image = rule.onNodeWithTag(DRAWER_TAG).captureToImage()
            val sourceRow = rule.onNode(
                hasText(fixture.source.feedSource.title) and
                    SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab),
            )
            sourceRow.assertIsDisplayed()
            val sourceBounds = sourceRow.fetchSemanticsNode().boundsInRoot
            assertTrue(
                sourceBounds.width >= (SOURCE_START_PADDING_DP + ICON_SIZE_DP) * densityScale,
                "The matched source semantics node must be the selectable row, not its title bounds: $sourceBounds",
            )
            assertRegionContrast(
                label = "No-logo icon for ${fixture.source.feedSource.title} in $mode",
                host = drawerBounds,
                region = Rect(
                    left = sourceBounds.left + SOURCE_START_PADDING_DP * densityScale,
                    top = sourceBounds.center.y - ICON_SIZE_DP * densityScale / 2,
                    right = sourceBounds.left + (SOURCE_START_PADDING_DP + ICON_SIZE_DP) * densityScale,
                    bottom = sourceBounds.center.y + ICON_SIZE_DP * densityScale / 2,
                ),
                image = image,
            )
        }

        rule.onNodeWithText(fixture.source.feedSource.title).performClick()
        assertEquals(FeedFilter.Source(fixture.source.feedSource), selectedFilter)
    }

    private var densityScale = 1f

    private fun setHost(
        theme: androidx.compose.runtime.MutableState<ThemeMode>,
        onSelected: (FeedFilter) -> Unit = {},
    ): DrawerFixture {
        val drawerState = DesktopLibraryFixture.load().drawerState()
        val source = drawerState.feedSourcesByCategory.values.flatten()
            .filterIsInstance<DrawerItem.DrawerFeedSource>()
            .first { it.feedSource.logoUrl == null }
        val category = requireNotNull(source.feedSource.category)
        rule.setContent {
            val density = LocalDensity.current
            SideEffect { densityScale = density.density }
            CompositionLocalProvider(
                LocalFeedFlowStrings provides EnFeedFlowStrings,
                LocalReduceMotion provides true,
            ) {
                FeedFlowTheme(
                    darkTheme = theme.value != ThemeMode.LIGHT,
                    useOledTheme = theme.value == ThemeMode.OLED,
                ) {
                    Box(
                        Modifier
                            .requiredSize(width = DRAWER_WIDTH, height = DRAWER_HEIGHT)
                            .background(JewelTheme.globalColors.toolwindowBackground)
                            .testTag(DRAWER_TAG),
                    ) {
                        DesktopDrawer(
                            modifier = Modifier.fillMaxSize(),
                            displayState = HomeDisplayState(
                                feedItems = persistentListOf(),
                                navDrawerState = drawerState,
                                unReadCount = 0,
                                isUnreadCountHidden = false,
                                feedUpdateStatus = NoFeedSourcesStatus,
                                feedFontSizes = FeedFontSizes(),
                                currentFeedFilter = FeedFilter.Timeline,
                                nextFeedDisplayState = NextFeedDisplayState.NextFeedDisplayDisabledState,
                                swipeActions = SwipeActions(SwipeActionType.NONE, SwipeActionType.NONE),
                                feedLayout = FeedLayout.LIST,
                                isGridLayoutEnabled = false,
                            ),
                            feedManagementActions = emptyActions(),
                            onFeedFilterSelected = onSelected,
                        )
                    }
                }
            }
        }
        return DrawerFixture(source, category)
    }

    private fun assertIconInEveryTheme(theme: androidx.compose.runtime.MutableState<ThemeMode>, description: String) {
        listOf(ThemeMode.LIGHT, ThemeMode.DARK, ThemeMode.OLED).forEach { mode ->
            rule.runOnIdle { theme.value = mode }
            val host = rule.onNodeWithTag(DRAWER_TAG).fetchSemanticsNode().boundsInRoot
            val image = rule.onNodeWithTag(DRAWER_TAG).captureToImage()
            assertNodeIconContrast(description, host, image)
        }
    }

    private fun assertNodeIconContrast(description: String, host: Rect, image: ImageBitmap) {
        val bounds = rule.onNodeWithContentDescription(description).fetchSemanticsNode().boundsInRoot
        assertRegionContrast(description, host, bounds, image)
    }

    private fun assertRegionContrast(
        label: String,
        host: Rect,
        region: Rect,
        image: ImageBitmap,
    ) {
        val pixels = image.toPixelMap()
        assertTrue(host.left >= 0 && host.top >= 0, "The fixture must fit inside the test surface: $host")
        val left = floor(region.left - host.left).toInt()
        val top = floor(region.top - host.top).toInt()
        val right = ceil(region.right - host.left).toInt()
        val bottom = ceil(region.bottom - host.top).toInt()
        assertTrue(
            left >= 0 && top >= 0 && right > left && bottom > top &&
                right <= pixels.width && bottom <= pixels.height,
            "The actual icon must fit in the captured fixture: $region within $host",
        )
        val background = pixels[left, top].luminance()
        val bestContrast = (left until right).maxOf { x ->
            (top until bottom).maxOf { y -> contrast(pixels[x, y].luminance(), background) }
        }
        println("Rendered icon contrast $label: $bestContrast")
        assertTrue(
            bestContrast >= MINIMUM_CONTRAST,
            "$label must have at least 3:1 contrast with its rendered surface; measured $bestContrast",
        )
    }

    private fun contrast(first: Float, second: Float): Float =
        (maxOf(first, second) + LUMINANCE_OFFSET) / (minOf(first, second) + LUMINANCE_OFFSET)

    private fun emptyActions() = FeedManagementActions(
        onAddFeedClick = {},
        onFeedFilterSelected = {},
        onEditFeedClick = {},
        onDeleteFeedSourceClick = {},
        onPinFeedClick = {},
        onEditCategoryClick = { _, _ -> },
        validateCategoryName = { _, _ -> CategoryNameValidationResult.VALID },
        onDeleteCategoryClick = {},
        onOpenWebsite = {},
        onMoveFeedSourcesToCategory = { _, _ -> },
        onReorderFeedSources = {},
        onReorderCategories = {},
        onDeleteAllFeedsInCategoryClick = {},
        onMarkAllReadForFeedSourceClick = {},
        onMarkAllReadForCategoryClick = {},
        onReorderPinnedFeedSources = {},
    )

    private data class DrawerFixture(
        val source: DrawerItem.DrawerFeedSource,
        val category: com.prof18.feedflow.core.model.FeedSourceCategory,
    )

    private companion object {
        const val DRAWER_TAG = "feed-management-icon-contrast-drawer"
        const val SOURCE_START_PADDING_DP = 16f
        const val ICON_SIZE_DP = 24f
        const val MINIMUM_CONTRAST = 3f
        const val LUMINANCE_OFFSET = 0.05f
        val DRAWER_WIDTH = 420.dp
        val DRAWER_HEIGHT = 700.dp
    }
}
