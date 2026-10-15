package com.prof18.feedflow.desktop.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.core.model.ArticleOpenMode
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.ThemeMode
import com.prof18.feedflow.desktop.home.drawer.DesktopFeedSourceDrawerItem
import com.prof18.feedflow.desktop.home.drawer.desktopDrawerItemVisualStyle
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.desktop.ui.components.FeedFlowTheme
import com.prof18.feedflow.i18n.EnFeedFlowStrings
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Text
import org.junit.Rule
import org.junit.Test
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DesktopFeedFetchWarningTest {
    @get:Rule
    val rule = createComposeRule()
    private var densityScale = 1f

    @Test
    fun `failed feed row exposes localized failure state without replacing selection or title`() {
        val source = mutableStateOf(feedSource(fetchFailed = true))
        val selected = mutableStateOf(true)
        setHost(source, selected)

        val row = rule.onNodeWithTag(ROW_TAG)
        row.assertIsSelected()
        rule.onNodeWithText(FEED_TITLE).assertIsDisplayed()
        assertEquals(
            EnFeedFlowStrings.feedFetchFailedTooltip,
            row.fetchSemanticsNode().config.getOrNull(SemanticsProperties.StateDescription),
        )
    }

    @Test
    fun `healthy feed row has no failure state description`() {
        val source = mutableStateOf(feedSource(fetchFailed = false))
        val selected = mutableStateOf(true)
        setHost(source, selected)

        val row = rule.onNodeWithTag(ROW_TAG)
        row.assertIsSelected()
        rule.onNodeWithText(FEED_TITLE).assertIsDisplayed()
        assertEquals(null, row.fetchSemanticsNode().config.getOrNull(SemanticsProperties.StateDescription))
    }

    @Test
    fun `failed healthy failed transition updates state while retaining row title and selection`() {
        val source = mutableStateOf(feedSource(fetchFailed = true))
        val selected = mutableStateOf(true)
        setHost(source, selected)
        val row = rule.onNodeWithTag(ROW_TAG)
        val descriptions = mutableListOf<String?>()

        fun observeRow() {
            row.assertIsSelected()
            rule.onNodeWithText(FEED_TITLE).assertIsDisplayed()
            descriptions += row.fetchSemanticsNode().config.getOrNull(SemanticsProperties.StateDescription)
        }

        observeRow()
        rule.runOnIdle { source.value = source.value.copy(fetchFailed = false) }
        observeRow()
        rule.runOnIdle { source.value = source.value.copy(fetchFailed = true) }
        observeRow()

        assertEquals(
            listOf(
                EnFeedFlowStrings.feedFetchFailedTooltip,
                null,
                EnFeedFlowStrings.feedFetchFailedTooltip,
            ),
            descriptions,
        )
    }

    @Test
    fun `warning icon has at least three to one contrast in every theme selected and unselected`() {
        val selected = mutableStateOf(false)
        val theme = mutableStateOf(ThemeMode.LIGHT)
        setContrastHost(theme, selected)
        val failures = mutableListOf<String>()
        listOf(false, true).forEach { isSelected ->
            rule.runOnIdle { selected.value = isSelected }
            listOf(ThemeMode.LIGHT, ThemeMode.DARK, ThemeMode.OLED).forEach { mode ->
                rule.runOnIdle { theme.value = mode }
                val contrast = measureWarningContrast()
                if (contrast < MINIMUM_CONTRAST) {
                    failures += "selected=$isSelected theme=$mode contrast=$contrast"
                }
            }
        }

        assertTrue(
            failures.isEmpty(),
            "Warning icon must have at least 3:1 contrast in every theme and selection state: " +
                failures.joinToString(),
        )
    }

    private fun setContrastHost(theme: MutableState<ThemeMode>, selected: MutableState<Boolean>) {
        val source = feedSource(fetchFailed = true)
        rule.setContent {
            val density = LocalDensity.current
            SideEffect { densityScale = density.density }
            CompositionLocalProvider(LocalFeedFlowStrings provides EnFeedFlowStrings) {
                FeedFlowTheme(
                    darkTheme = theme.value != ThemeMode.LIGHT,
                    useOledTheme = theme.value == ThemeMode.OLED,
                ) {
                    Box(
                        Modifier
                            .requiredSize(width = HOST_WIDTH, height = HOST_HEIGHT)
                            .background(JewelTheme.globalColors.toolwindowBackground)
                            .testTag(HOST_TAG),
                    ) {
                        warningRow(source, selected.value)
                    }
                }
            }
        }
    }

    private fun measureWarningContrast(): Float {
        val capture = captureWarningLayout()
        assertHostCaptureBounds(capture.hostBounds, capture.image)
        assertWarningCropBounds(capture)
        return warningContrast(capture)
    }

    private fun captureWarningLayout(): WarningLayoutCapture {
        val hostBounds = rule.onNodeWithTag(HOST_TAG).fetchSemanticsNode().boundsInRoot
        val rowBounds = rule.onNodeWithTag(ROW_TAG).fetchSemanticsNode().boundsInRoot
        val labelBounds = rule.onNodeWithText(FEED_TITLE, useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
        val region = Rect(
            left = rowBounds.left + START_PADDING_DP * densityScale,
            top = rowBounds.center.y - WARNING_REGION_HEIGHT_DP * densityScale / 2,
            right = labelBounds.left - LABEL_GAP_DP * densityScale,
            bottom = rowBounds.center.y + WARNING_REGION_HEIGHT_DP * densityScale / 2,
        )
        return WarningLayoutCapture(
            hostBounds = hostBounds,
            rowBounds = rowBounds,
            labelBounds = labelBounds,
            warningRegion = region,
            image = rule.onNodeWithTag(HOST_TAG).captureToImage(),
        )
    }

    private fun assertHostCaptureBounds(host: Rect, image: ImageBitmap) {
        assertTrue(host.width > 0f && host.height > 0f, "Fixture host must be laid out")
        assertTrue(host.left >= 0f && host.top >= 0f, "Host must not be clipped at the origin")
        assertEquals(ceil(host.width).toInt(), image.width, "Capture must cover the entire host width")
        assertEquals(ceil(host.height).toInt(), image.height, "Capture must cover the entire host height")
    }

    private fun assertWarningCropBounds(capture: WarningLayoutCapture) {
        val row = capture.rowBounds
        val host = capture.hostBounds
        val label = capture.labelBounds
        val region = capture.warningRegion
        assertTrue(
            row.left >= host.left && row.right <= host.right && row.top >= host.top && row.bottom <= host.bottom,
            "Warning row must be fully inside the captured host: $row in $host",
        )
        assertTrue(
            label.left > row.left && label.right <= row.right && region.right < label.left &&
                region.left >= row.left && region.right <= row.right &&
                region.width >= MIN_WARNING_REGION_DP * densityScale,
            "Warning crop must cover a leading area before the label: $region, row=$row, label=$label",
        )
    }

    private fun setHost(source: MutableState<FeedSource>, selected: MutableState<Boolean>) {
        rule.setContent {
            CompositionLocalProvider(LocalFeedFlowStrings provides EnFeedFlowStrings) {
                FeedFlowTheme(darkTheme = false, useOledTheme = false) {
                    Box(
                        Modifier
                            .requiredSize(width = HOST_WIDTH, height = HOST_HEIGHT)
                            .background(JewelTheme.globalColors.toolwindowBackground)
                            .testTag(HOST_TAG),
                    ) {
                        warningRow(source.value, selected.value)
                    }
                }
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun warningRow(source: FeedSource, selected: Boolean) {
        DesktopFeedSourceDrawerItem(
            feedSource = source,
            label = { Text(source.title) },
            selected = selected,
            drawerItemVisualStyle = desktopDrawerItemVisualStyle(),
            isMultiSelected = false,
            onClick = {},
            icon = {},
            onEditFeedClick = {},
            onDeleteFeedSourceClick = {},
            onPinFeedClick = {},
            onChangeFeedCategoryClick = {},
            onOpenWebsite = {},
            onMarkAllReadForFeedSourceClick = {},
            unreadCount = 0,
            modifier = Modifier.testTag(ROW_TAG),
        )
    }

    private fun warningContrast(capture: WarningLayoutCapture): Float {
        val pixels = capture.image.toPixelMap()
        val host = capture.hostBounds
        val row = capture.rowBounds
        val region = capture.warningRegion
        val scaleX = pixels.width / host.width
        val scaleY = pixels.height / host.height
        val left = floor((region.left - host.left) * scaleX).toInt()
        val top = floor((region.top - host.top) * scaleY).toInt()
        val right = ceil((region.right - host.left) * scaleX).toInt()
        val bottom = ceil((region.bottom - host.top) * scaleY).toInt()
        assertTrue(left >= 0 && top >= 0 && right <= pixels.width && bottom <= pixels.height)
        assertTrue(right > left && bottom > top)
        val backgroundX = floor(
            (row.right - host.left - BACKGROUND_SAMPLE_INSET_DP * densityScale) * scaleX,
        ).toInt()
        val backgroundY = floor((row.center.y - host.top) * scaleY).toInt()
        assertTrue(backgroundX in 0 until pixels.width && backgroundY in 0 until pixels.height)
        val background = pixels[backgroundX, backgroundY].luminance()
        return (left until right).maxOf { x ->
            (top until bottom).maxOf { y ->
                val foreground = pixels[x, y].luminance()
                (maxOf(foreground, background) + LUMINANCE_OFFSET) /
                    (minOf(foreground, background) + LUMINANCE_OFFSET)
            }
        }
    }

    private fun feedSource(fetchFailed: Boolean) = FeedSource(
        id = "fetch-warning-source",
        url = "https://example.com/feed.xml",
        title = FEED_TITLE,
        category = null,
        lastSyncTimestamp = null,
        logoUrl = null,
        websiteUrl = null,
        fetchFailed = fetchFailed,
        articleOpenMode = ArticleOpenMode.DEFAULT,
        isHiddenFromTimeline = false,
        isPinned = false,
        isNotificationEnabled = false,
        isHideImagesEnabled = false,
    )

    private data class WarningLayoutCapture(
        val hostBounds: Rect,
        val rowBounds: Rect,
        val labelBounds: Rect,
        val warningRegion: Rect,
        val image: ImageBitmap,
    )

    private companion object {
        const val HOST_TAG = "fetch-warning-host"
        const val ROW_TAG = "fetch-warning-row"
        const val FEED_TITLE = "Fetch warning fixture"
        const val START_PADDING_DP = 4f
        const val WARNING_REGION_HEIGHT_DP = 24f
        const val MIN_WARNING_REGION_DP = 10f
        const val LABEL_GAP_DP = 2f
        const val BACKGROUND_SAMPLE_INSET_DP = 4f
        const val MINIMUM_CONTRAST = 3f
        const val LUMINANCE_OFFSET = 0.05f
        val HOST_WIDTH = 320.dp
        val HOST_HEIGHT = 96.dp
    }
}
