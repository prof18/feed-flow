package com.prof18.feedflow.desktop.settings

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.desktop.testing.NativeDesktopWindowFixture
import com.prof18.feedflow.desktop.testing.NativeDesktopWindowFixtureRule
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.desktop.ui.components.FeedFlowTheme
import com.prof18.feedflow.i18n.EnFeedFlowStrings
import kotlinx.coroutines.runBlocking
import org.jetbrains.jewel.ui.component.OutlinedButton
import org.jetbrains.jewel.ui.component.Text
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import java.awt.Dialog
import java.awt.GraphicsEnvironment
import java.awt.Window
import java.util.concurrent.atomic.AtomicReference
import javax.swing.SwingUtilities
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class DesktopSettingsWindowTest {
    private val density = AtomicReference<Float>()
    private val activationRequest = mutableStateOf(0)
    private val fixture = NativeDesktopWindowFixture {
        CompositionLocalProvider(LocalFeedFlowStrings provides EnFeedFlowStrings) {
            FeedFlowTheme(darkTheme = false, useOledTheme = false) {
                density.set(LocalDensity.current.density)
                DesktopSettingsDialog(onCloseRequest = {}, activationRequest = activationRequest.value) { modifier ->
                    Text("Settings window content", modifier = modifier.testTag(WINDOW_CONTENT_TAG))
                }
            }
        }
    }

    private val nativeWindow = NativeDesktopWindowFixtureRule(fixture)
    private val compose = createComposeRule()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(nativeWindow).around(compose)

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
    fun `raising Settings preserves the existing native window and user size`() {
        val window = awaitSettingsWindow()
        val initialSize = readWindowSize(window)
        SwingUtilities.invokeAndWait {
            window.setSize(initialSize.width + RESIZE_WIDTH_DP, initialSize.height + RESIZE_HEIGHT_DP)
        }
        val resized = readWindowSize(window)
        compose.runOnIdle { activationRequest.value++ }
        compose.waitForIdle()
        assertSame(window, awaitSettingsWindow())
        val afterActivation = readWindowSize(window)
        assertEquals(resized.width, afterActivation.width)
        assertEquals(resized.height, afterActivation.height)
        var focused = false
        compose.waitUntil(timeoutMillis = WINDOW_START_TIMEOUT_MS) {
            SwingUtilities.invokeAndWait { focused = window.isFocused }
            focused
        }
    }

    @Test
    fun `settings dialog is resizable and enforces desktop minimum size`() {
        val window = awaitSettingsWindow()
        val initialSize = readWindowSize(window)
        val scale = initialSize.scale
        val densityScale = requireNotNull(density.get()) / scale
        // The OS keeps a window inside the usable screen, so small displays (CI VMs) get a smaller initial size.
        val screen = GraphicsEnvironment.getLocalGraphicsEnvironment().maximumWindowBounds
        assertTrue(initialSize.isResizable)
        assertTrue(initialSize.width >= minOf((INITIAL_WIDTH_DP * densityScale).toInt(), screen.width))
        assertTrue(initialSize.height >= minOf((INITIAL_HEIGHT_DP * densityScale).toInt(), screen.height))
        assertTrue(initialSize.minimumWidth >= (MINIMUM_WIDTH_DP * densityScale).toInt())
        assertTrue(initialSize.minimumHeight >= (MINIMUM_HEIGHT_DP * densityScale).toInt())

        SwingUtilities.invokeAndWait {
            window.setSize(initialSize.minimumWidth, initialSize.minimumHeight)
        }
        compose.waitUntil(timeoutMillis = WINDOW_RESIZE_TIMEOUT_MS) {
            val size = readWindowSize(window)
            size.width == initialSize.minimumWidth && size.height == initialSize.minimumHeight
        }
        val resizeStart = readWindowSize(window)
        SwingUtilities.invokeAndWait {
            window.setSize(
                resizeStart.width + (RESIZE_WIDTH_DP * densityScale).toInt(),
                resizeStart.height + (RESIZE_HEIGHT_DP * densityScale).toInt(),
            )
        }
        compose.waitUntil(timeoutMillis = WINDOW_RESIZE_TIMEOUT_MS) {
            val size = readWindowSize(window)
            size.width >= resizeStart.width + (RESIZE_WIDTH_DP * densityScale).toInt() &&
                size.height >= resizeStart.height + (RESIZE_HEIGHT_DP * densityScale).toInt()
        }
        val enlargedSize = readWindowSize(window)
        assertTrue(
            enlargedSize.height >= resizeStart.height + (RESIZE_HEIGHT_DP * densityScale).toInt(),
            "Resize from $resizeStart produced $enlargedSize at density $densityScale",
        )

        SwingUtilities.invokeAndWait {
            window.setSize(
                initialSize.minimumWidth - (BELOW_MINIMUM_DP * densityScale).toInt(),
                initialSize.minimumHeight - (BELOW_MINIMUM_DP * densityScale).toInt(),
            )
        }
        compose.waitUntil(timeoutMillis = WINDOW_RESIZE_TIMEOUT_MS) {
            val size = readWindowSize(window)
            size.width >= initialSize.minimumWidth && size.height >= initialSize.minimumHeight
        }
        val clampedSize = readWindowSize(window)
        assertTrue(clampedSize.width >= initialSize.minimumWidth)
        assertTrue(clampedSize.height >= initialSize.minimumHeight)
    }

    @Composable
    private fun SettingsContent(
        initialCategory: DesktopSettingsCategory,
        onCategorySelected: (DesktopSettingsCategory) -> Unit = {},
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
                    content = paneContent,
                )
            }
        }
    }

    private fun awaitSettingsWindow(): Dialog {
        var window: Dialog? = null
        compose.waitUntil(timeoutMillis = WINDOW_START_TIMEOUT_MS) {
            SwingUtilities.invokeAndWait { window = settingsWindowOrNull() }
            window != null
        }
        return requireNotNull(window)
    }

    private fun settingsWindowOrNull(): Dialog? = Window.getWindows()
        .filterIsInstance<Dialog>()
        .singleOrNull { it.isVisible && it.title == EnFeedFlowStrings.settingsTitle }

    private fun readWindowSize(window: Dialog): NativeWindowSize {
        var size: NativeWindowSize? = null
        SwingUtilities.invokeAndWait {
            val transform = window.graphicsConfiguration.defaultTransform
            size = NativeWindowSize(
                width = window.width,
                height = window.height,
                minimumWidth = window.minimumSize.width,
                minimumHeight = window.minimumSize.height,
                scale = transform.scaleX,
                isResizable = window.isResizable,
            )
        }
        return requireNotNull(size)
    }

    private companion object {
        const val DETAIL_TAG = "settings-detail"
        const val DETAIL_ACTION_TAG = "settings-detail-action"
        const val SCROLLING_PANE_TAG = "settings-scroll-pane"
        const val WINDOW_CONTENT_TAG = "settings-window-content"
        const val INITIAL_WIDTH_DP = 960
        const val INITIAL_HEIGHT_DP = 720
        const val MINIMUM_WIDTH_DP = 680
        const val MINIMUM_HEIGHT_DP = 520
        const val RESIZE_WIDTH_DP = 80
        const val RESIZE_HEIGHT_DP = 60
        const val BELOW_MINIMUM_DP = 100
        const val SAVED_SCROLL_POSITION = 80
        const val WINDOW_START_TIMEOUT_MS = 10_000L
        const val WINDOW_RESIZE_TIMEOUT_MS = 2_000L
    }
}

private data class NativeWindowSize(
    val width: Int,
    val height: Int,
    val minimumWidth: Int,
    val minimumHeight: Int,
    val scale: Double,
    val isResizable: Boolean,
)
