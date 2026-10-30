@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package com.prof18.feedflow.desktop.ui.components

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.core.model.CategoryId
import com.prof18.feedflow.core.model.CategoryName
import com.prof18.feedflow.core.model.CategoryNameValidationResult
import com.prof18.feedflow.core.model.DrawerItem
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedFontSizes
import com.prof18.feedflow.core.model.FeedItem
import com.prof18.feedflow.core.model.FeedLayout
import com.prof18.feedflow.core.model.FeedSourceCategory
import com.prof18.feedflow.core.model.NavDrawerState
import com.prof18.feedflow.core.model.NoFeedSourcesStatus
import com.prof18.feedflow.core.model.SwipeActionType
import com.prof18.feedflow.core.model.SwipeActions
import com.prof18.feedflow.desktop.testing.DesktopLibraryFixture
import com.prof18.feedflow.desktop.ui.FeedManagementActions
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.i18n.EnFeedFlowStrings
import com.prof18.feedflow.shared.presentation.model.HomeDisplayState
import com.prof18.feedflow.shared.presentation.model.NextFeedDisplayState
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import org.junit.Rule
import org.junit.Test
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.absoluteValue
import kotlin.test.assertTrue

class FeedSidebarDensityTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `category header and source stay compact and legible across sidebar widths and large text`() {
        val category = FeedSourceCategory(id = "density-category", title = "Science", position = 0)
        val source = DesktopLibraryFixture.load().sources.first().feedSource.copy(
            id = "density-feed",
            title = "A Very Long Source Name That Must Remain on One Line",
            category = category,
            logoUrl = null,
        )
        val categoryWrapper = DrawerItem.DrawerFeedSource.FeedSourceCategoryWrapper(category)
        val drawerState = NavDrawerState(
            feedSourcesByCategory = persistentMapOf(
                categoryWrapper to listOf(DrawerItem.DrawerFeedSource(source, unreadCount = 12)),
            ),
        )
        val displayState = emptyHomeDisplayState().copy(navDrawerState = drawerState)
        val managementActions = FeedManagementActions(
            onAddFeedClick = {},
            onFeedFilterSelected = {},
            onEditFeedClick = {},
            onDeleteFeedSourceClick = {},
            onPinFeedClick = {},
            onEditCategoryClick = { _: CategoryId, _: CategoryName -> },
            validateCategoryName = { _: CategoryId?, _: CategoryName -> CategoryNameValidationResult.VALID },
            onDeleteCategoryClick = {},
            onOpenWebsite = {},
        )
        val layoutConfig = mutableStateOf(200 to 1f)

        rule.setContent {
            CompositionLocalProvider(
                LocalFeedFlowStrings provides EnFeedFlowStrings,
                LocalDensity provides Density(LocalDensity.current.density, layoutConfig.value.second),
            ) {
                FeedFlowTheme(darkTheme = true, useOledTheme = false) {
                    FeedSidebar(
                        displayState = displayState,
                        feedManagementActions = managementActions,
                        onFeedFilterSelected = {},
                        modifier = Modifier
                            .width(layoutConfig.value.first.dp)
                            .height(760.dp)
                            .testTag(SIDEBAR_TAG),
                    )
                }
            }
        }

        val configurations = listOf(1f, 2f).flatMap { scale ->
            listOf(200, 248, 360).map { width -> width to scale }
        }
        for ((sidebarWidth, fontScale) in configurations) {
            rule.runOnIdle { layoutConfig.value = sidebarWidth to fontScale }
            rule.waitForIdle()

            val categoryBounds = rule.onNodeWithText(category.title, useUnmergedTree = true)
                .fetchSemanticsNode().boundsInRoot
            val countBounds = rule.onAllNodesWithText("12", useUnmergedTree = true).onFirst()
                .fetchSemanticsNode().boundsInRoot
            val allCountBounds = rule.onAllNodesWithText("12", useUnmergedTree = true)
                .fetchSemanticsNodes().map { it.boundsInRoot }
            val disclosure = rule.onNodeWithContentDescription(
                EnFeedFlowStrings.desktopCollapseFeedCategory(category.title),
            ).fetchSemanticsNode().boundsInRoot
            val sourceBounds = rule.onNodeWithText(source.title, useUnmergedTree = true)
                .fetchSemanticsNode().boundsInRoot
            val tab = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)
            val categoryRowBounds = rule.onNode(tab and hasAnyDescendant(hasText(category.title)), true)
                .fetchSemanticsNode().boundsInRoot
            val sourceRowBounds = rule.onNode(tab and hasAnyDescendant(hasText(source.title)), true)
                .fetchSemanticsNode().boundsInRoot
            val availableWidthPx = with(rule.density) { (sidebarWidth.dp - 12.dp).toPx() }
            val alignmentTolerancePx = with(rule.density) { 3.dp.toPx() }

            captureSidebar(width = sidebarWidth, fontScale = fontScale)
            val scenario = "width=$sidebarWidth, scale=$fontScale"

            assertTrue(
                (categoryBounds.center.y - countBounds.center.y).absoluteValue <= alignmentTolerancePx,
                "Category label and unread count should share a centered row ($scenario)",
            )
            assertTrue(
                allCountBounds.size == 2 &&
                    (sourceRowBounds.right - allCountBounds[1].right) <= with(rule.density) { 12.dp.toPx() } &&
                    allCountBounds[1].right <= sourceRowBounds.right,
                "Source counts should use the trailing row space without reserving a disclosure slot ($scenario)",
            )
            assertTrue(
                (categoryBounds.center.y - disclosure.center.y).absoluteValue <= alignmentTolerancePx,
                "Category label and disclosure should share a centered row ($scenario)",
            )
            assertTrue(
                categoryBounds.right <= availableWidthPx && disclosure.right <= availableWidthPx,
                "Category controls should fit inside the padded sidebar ($scenario)",
            )
            assertTrue(
                sourceBounds.height <= categoryBounds.height * 1.5f,
                "Source title should occupy one visual line ($scenario)",
            )
            assertTrue(
                sourceBounds.left > categoryBounds.left && sourceBounds.right <= availableWidthPx,
                "Source title should be indented and remain inside the sidebar ($scenario)",
            )
            if (fontScale == 1f) {
                val compactRowLimitPx = with(rule.density) { 40.dp.toPx() }
                assertTrue(
                    categoryRowBounds.height <= compactRowLimitPx && sourceRowBounds.height <= compactRowLimitPx,
                    "Category and source rows should remain compact at width=$sidebarWidth",
                )
            }
        }
    }

    private fun captureSidebar(width: Int, fontScale: Float) {
        val pixels = rule.onNodeWithTag(SIDEBAR_TAG).captureToImage().toPixelMap()
        val image = BufferedImage(pixels.width, pixels.height, BufferedImage.TYPE_INT_ARGB)
        for (y in 0 until pixels.height) {
            for (x in 0 until pixels.width) image.setRGB(x, y, pixels[x, y].toArgb())
        }
        val scaleLabel = if (fontScale == 1f) "1x" else "2x"
        val output = File("build/reports/tests/sidebar-density/$width-$scaleLabel.png")
        output.parentFile.mkdirs()
        ImageIO.write(image, "png", output)
    }

    private fun emptyHomeDisplayState() = HomeDisplayState(
        feedItems = persistentListOf<FeedItem>(),
        navDrawerState = NavDrawerState(),
        unReadCount = 0,
        isUnreadCountHidden = false,
        feedUpdateStatus = NoFeedSourcesStatus,
        feedFontSizes = FeedFontSizes(),
        currentFeedFilter = FeedFilter.Timeline,
        nextFeedDisplayState = NextFeedDisplayState.NextFeedDisplayDisabledState,
        swipeActions = SwipeActions(SwipeActionType.NONE, SwipeActionType.NONE),
        feedLayout = FeedLayout.LIST,
        isGridLayoutEnabled = false,
    )

    private companion object {
        const val SIDEBAR_TAG = "sidebar-density-capture"
    }
}
