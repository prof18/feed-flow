@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package com.prof18.feedflow.desktop.importexport

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.requestFocus
import com.prof18.feedflow.core.model.FeedImportExportState
import com.prof18.feedflow.desktop.settings.DesktopSettingsDialog
import com.prof18.feedflow.desktop.testing.ImportExportE2eIds
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.desktop.ui.components.FeedFlowTheme
import com.prof18.feedflow.i18n.EnFeedFlowStrings
import org.jetbrains.jewel.ui.component.OutlinedButton
import org.jetbrains.jewel.ui.component.Text
import org.junit.Rule
import org.junit.Test
import java.awt.Component
import java.awt.Container
import java.awt.Dialog
import java.awt.FileDialog
import java.awt.Window
import java.awt.event.KeyEvent
import java.io.File
import java.util.concurrent.atomic.AtomicReference
import javax.swing.SwingUtilities
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class DesktopImportExportWindowTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `all four native file picker requests belong to the import export window`() {
        val pickerOwner = AtomicReference<Dialog>()
        content {
            SettingsImportExportWindow(onCloseRequest = {}) { owner ->
                SideEffect { pickerOwner.set(owner) }
                OutlinedButton(onClick = {}, modifier = Modifier.testTag(CONTENT_TAG)) { Text("Fixture action") }
            }
        }
        rule.waitUntilExactlyOneExists(hasTestTag(CONTENT_TAG), timeoutMillis = 5_000)
        SwingUtilities.invokeAndWait {
            val owner = visibleWindow()
            listOf(true to null, false to "fixture.opml", true to null, false to "fixture.csv").forEach { request ->
                val picker = createImportExportFileDialog(
                    parent = requireNotNull(pickerOwner.get()),
                    dialogTitle = "Fixture picker",
                    onCloseRequest = {},
                    exportFileName = request.second,
                    isLoadDialog = request.first,
                )
                try {
                    assertSame(owner, picker.owner)
                    assertEquals(if (request.first) FileDialog.LOAD else FileDialog.SAVE, picker.mode)
                    if (request.second != null) assertEquals(request.second, picker.file)
                } finally {
                    picker.dispose()
                }
            }
        }
    }

    @Test
    fun `native Escape closes only the import export window without invoking an operation`() {
        val visible = mutableStateOf(true)
        var operations = 0
        content {
            if (visible.value) {
                SettingsImportExportWindow(onCloseRequest = { visible.value = false }) {
                    OutlinedButton(onClick = { operations++ }, modifier = Modifier.testTag(CONTENT_TAG)) {
                        Text("Fixture operation")
                    }
                }
            }
        }
        rule.waitUntilExactlyOneExists(hasTestTag(CONTENT_TAG), timeoutMillis = 5_000)
        rule.onNodeWithTag(CONTENT_TAG).requestFocus()
        SwingUtilities.invokeAndWait {
            val input = descendants(visibleWindow()).single { component ->
                component.keyListeners.any { it.javaClass.name.contains("ComposeSceneMediator") }
            }
            val event = KeyEvent(input, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_ESCAPE)
            input.keyListeners.forEach { it.keyPressed(event) }
        }
        rule.waitUntil(timeoutMillis = 5_000) { !visible.value }
        rule.runOnIdle { assertEquals(0, operations) }
    }

    @Test
    fun `native picker cancellation returns null once and leaves the initiating action focused`() {
        val pickerVisible = mutableStateOf(false)
        val results = mutableListOf<File?>()
        var dismissals = 0
        content {
            SettingsImportExportWindow(onCloseRequest = { dismissals++ }) { owner ->
                OutlinedButton(
                    onClick = { pickerVisible.value = true },
                    modifier = Modifier.testTag(CONTENT_TAG),
                ) { Text("Fixture import") }
                if (pickerVisible.value) {
                    NativeImportExportFileDialog(
                        dialogTitle = CANCEL_PICKER_TITLE,
                        parent = owner,
                        isLoadDialog = true,
                        onCloseRequest = { result ->
                            results.add(result)
                            pickerVisible.value = false
                        },
                    )
                }
            }
        }
        rule.waitUntilExactlyOneExists(hasTestTag(CONTENT_TAG), timeoutMillis = 5_000)
        val initiator = rule.onNodeWithTag(CONTENT_TAG)
        initiator.requestFocus()
        try {
            initiator.performSemanticsAction(SemanticsActions.OnClick) { it() }
            rule.waitUntil(timeoutMillis = 5_000) {
                var showing = false
                SwingUtilities.invokeAndWait { showing = cancelPicker()?.isVisible == true }
                showing
            }
            SwingUtilities.invokeAndWait {
                val picker = requireNotNull(cancelPicker())
                assertSame(visibleWindow(), picker.owner)
                picker.isVisible = false
            }
            rule.waitUntil(timeoutMillis = 5_000) { !pickerVisible.value }
            rule.runOnIdle {
                assertEquals(listOf<File?>(null), results)
                assertEquals(0, dismissals)
            }
            initiator.assertIsDisplayed().assertIsFocused()
            SwingUtilities.invokeAndWait { assertTrue(visibleWindow().isVisible) }
        } finally {
            SwingUtilities.invokeAndWait { cancelPicker()?.dispose() }
        }
    }

    private fun cancelPicker(): FileDialog? = Window.getWindows().filterIsInstance<FileDialog>().singleOrNull {
        it.title == CANCEL_PICKER_TITLE && it.isDisplayable
    }

    @Test
    fun `filter belongs to import export and Escape restores its initiating action without export`() {
        var exports = 0
        var dismissals = 0
        content {
            SettingsImportExportWindow(onCloseRequest = { dismissals++ }) {
                ImportExportContent(
                    feedImportExportState = FeedImportExportState.Idle,
                    onRetryClick = {},
                    onChooseAnotherFileClick = {},
                    onDoneClick = {},
                    onImportClick = {},
                    onExportClick = {},
                    onImportArticlesClick = {},
                    onExportArticlesClick = { exports++ },
                )
            }
        }
        val initiator = rule.onNodeWithTag(ImportExportE2eIds.EXPORT_ARTICLES)
        initiator.requestFocus().performSemanticsAction(SemanticsActions.OnClick) { it() }
        rule.waitUntilExactlyOneExists(
            androidx.compose.ui.test.hasText(EnFeedFlowStrings.articlesExportFilterAll),
            timeoutMillis = 5_000,
        )
        SwingUtilities.invokeAndWait {
            val filter = Window.getWindows().filterIsInstance<Dialog>().single {
                it.isVisible && it.title == EnFeedFlowStrings.articlesExportFilterTitle
            }
            assertSame(visibleWindow(), filter.owner)
            val input = descendants(filter).single { component ->
                component.keyListeners.any { it.javaClass.name.contains("ComposeSceneMediator") }
            }
            val event = KeyEvent(input, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_ESCAPE)
            input.keyListeners.forEach { it.keyPressed(event) }
        }
        rule.waitUntil(timeoutMillis = 5_000) {
            var closed = false
            SwingUtilities.invokeAndWait {
                closed = Window.getWindows().filterIsInstance<Dialog>().none {
                    it.isVisible && it.title == EnFeedFlowStrings.articlesExportFilterTitle
                }
            }
            closed
        }
        initiator.assertIsFocused()
        rule.runOnIdle {
            assertEquals(0, exports)
            assertEquals(0, dismissals)
        }
    }

    private fun visibleWindow(): Dialog = Window.getWindows().filterIsInstance<Dialog>().single {
        it.isVisible && it.title == EnFeedFlowStrings.settingsTitle
    }

    private fun descendants(component: Component): Sequence<Component> = sequence {
        yield(component)
        if (component is Container) component.components.forEach { yieldAll(descendants(it)) }
    }

    private fun content(body: @Composable () -> Unit) {
        rule.setContent {
            CompositionLocalProvider(LocalFeedFlowStrings provides EnFeedFlowStrings) {
                FeedFlowTheme(darkTheme = false, useOledTheme = false, content = body)
            }
        }
    }

    private companion object {
        const val CONTENT_TAG = "import-export-window-fixture-action"
        const val CANCEL_PICKER_TITLE = "FeedFlow native cancellation fixture"
    }
}

@Composable
private fun SettingsImportExportWindow(onCloseRequest: () -> Unit, content: @Composable (Dialog) -> Unit) {
    DesktopSettingsDialog(onCloseRequest = onCloseRequest) { content(window) }
}
