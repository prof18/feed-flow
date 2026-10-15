package com.prof18.feedflow.desktop.reaadermode

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeDialog
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import coil3.PlatformContext
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.ReaderModeData
import com.prof18.feedflow.core.model.ReaderModeDefaults
import com.prof18.feedflow.core.model.ReaderModeState
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.desktop.ui.components.FeedFlowTheme
import com.prof18.feedflow.i18n.EnFeedFlowStrings
import org.junit.After
import org.junit.Rule
import org.junit.Test
import java.awt.Component
import java.awt.Container
import java.awt.Window
import java.awt.event.KeyEvent
import javax.swing.SwingUtilities
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReaderTextSettingsPositionTest {
    @get:Rule
    val rule = createComposeRule()

    private val imageLoader = ImageLoader.Builder(PlatformContext.INSTANCE).build()

    @Test
    fun `native Escape closes text settings and retains the long document position and live values`() {
        val fontSize = mutableIntStateOf(ReaderModeDefaults.FONT_SIZE)
        val lineHeight = mutableIntStateOf(ReaderModeDefaults.LINE_HEIGHT)
        val readerState = ReaderModeState.Success(
            ReaderModeData(
                id = FeedItemId("long-reader-fixture"),
                title = "Long reader fixture",
                content = (1..PARAGRAPH_COUNT).joinToString("\n\n") { "Paragraph $it has text for reading position." },
                url = "",
                baseUrl = "",
                fontSize = ReaderModeDefaults.FONT_SIZE,
                lineHeight = ReaderModeDefaults.LINE_HEIGHT,
                isBookmarked = false,
            ),
        )
        rule.setContent {
            CompositionLocalProvider(LocalFeedFlowStrings provides EnFeedFlowStrings) {
                FeedFlowTheme(darkTheme = false, useOledTheme = false) {
                    Column(Modifier.size(width = 560.dp, height = 640.dp)) {
                        ReaderModeToolbar(
                            readerModeState = readerState,
                            fontSize = fontSize.intValue,
                            lineHeight = lineHeight.intValue,
                            navigateBack = {},
                            showBackButton = false,
                            onToggleDetailFullscreen = null,
                            isDetailFullscreen = false,
                            openInBrowser = {},
                            onShareClick = {},
                            onArchiveClick = {},
                            onCommentsClick = {},
                            onFontSizeChange = { fontSize.intValue = it },
                            onLineHeightChange = { lineHeight.intValue = it },
                            onBookmarkClick = { _, _ -> },
                            onToggleContentSource = {},
                        )
                        DesktopReaderContent(
                            state = readerState,
                            fontSize = fontSize.intValue,
                            lineHeight = lineHeight.intValue,
                            imageLoader = imageLoader,
                            onOpenInBrowser = {},
                            onHoveredLinkChange = {},
                            modifier = Modifier.weight(1f).fillMaxSize(),
                        )
                    }
                }
            }
        }
        val target = "Paragraph $TARGET_PARAGRAPH has text for reading position."
        rule.waitUntil(timeoutMillis = 5_000) { rule.onAllNodesWithText(target).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText(target).performScrollTo().assertIsDisplayed()
        val positionBefore = documentPosition()
        assertTrue(positionBefore > 0f)

        rule.onNodeWithContentDescription(EnFeedFlowStrings.readerModeTextSettings).performClick()
        rule.onNodeWithContentDescription(EnFeedFlowStrings.readerModeFontSize)
            .performSemanticsAction(SemanticsActions.SetProgress) { it(NEW_FONT_SIZE.toFloat()) }
        rule.waitForIdle()
        val positionWithSettings = documentPosition()
        assertEquals(positionBefore, positionWithSettings)

        SwingUtilities.invokeAndWait {
            val dialog = Window.getWindows().filterIsInstance<ComposeDialog>()
                .single { it.isShowing && it.title == EnFeedFlowStrings.readerModeTextSettings }
            val input = descendants(dialog).single { component ->
                component.keyListeners.any { it.javaClass.name.contains("ComposeSceneMediator") }
            }
            val escape = KeyEvent(input, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_ESCAPE)
            input.keyListeners.forEach { it.keyPressed(escape) }
        }
        rule.waitForIdle()
        rule.onNodeWithContentDescription(EnFeedFlowStrings.readerModeFontSize).assertDoesNotExist()
        assertEquals(positionWithSettings, documentPosition())
        rule.runOnIdle { assertEquals(NEW_FONT_SIZE, fontSize.intValue) }
    }

    private fun documentPosition(): Float = rule.onNodeWithTag(READER_DOCUMENT_TAG)
        .fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange].value()

    private fun descendants(component: Component): Sequence<Component> = sequence {
        yield(component)
        if (component is Container) component.components.forEach { yieldAll(descendants(it)) }
    }

    @After
    fun closeImageLoader() { imageLoader.shutdown() }

    private companion object {
        const val PARAGRAPH_COUNT = 80
        const val TARGET_PARAGRAPH = 40
        const val NEW_FONT_SIZE = 22
    }
}
