package com.prof18.feedflow.desktop.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.desktop.ui.components.FeedFlowTheme
import com.prof18.feedflow.i18n.EnFeedFlowStrings
import org.jetbrains.jewel.ui.component.OutlinedButton
import org.jetbrains.jewel.ui.component.Text
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

class ModalDrawerTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `narrow sidebar traps input and Escape restores pane interaction`() {
        val open = mutableStateOf(false)
        var paneClicks = 0
        rule.setContent {
            CompositionLocalProvider(LocalFeedFlowStrings provides EnFeedFlowStrings) {
                FeedFlowTheme(darkTheme = false, useOledTheme = false) {
                    Box(Modifier.size(width = 560.dp, height = 640.dp)) {
                        ModalDrawer(
                            isOpen = open.value,
                            onDismiss = { open.value = false },
                            drawerContent = {
                                OutlinedButton(
                                    onClick = { open.value = false },
                                    modifier = Modifier.testTag("drawer-action"),
                                ) {
                                    Text(EnFeedFlowStrings.drawerTitleTimeline)
                                }
                            },
                            paneContent = {
                                OutlinedButton(onClick = { paneClicks++ }, modifier = Modifier.testTag("pane-action")) {
                                    Text(EnFeedFlowStrings.addFeed)
                                }
                            },
                        )
                    }
                }
            }
        }
        rule.onNodeWithTag("pane-action").requestFocus()
        rule.runOnIdle { open.value = true }
        rule.onNodeWithTag("drawer-action").performKeyInput { pressKey(Key.Escape) }
        rule.runOnIdle { assertEquals(false, open.value) }
        rule.onNodeWithTag("pane-action").assertIsFocused()
        rule.onNodeWithTag("pane-action").performClick()
        rule.runOnIdle { assertEquals(1, paneClicks) }
        rule.runOnIdle { open.value = true }
        rule.onNodeWithTag("drawer-action").performClick()
        rule.runOnIdle { assertEquals(false, open.value) }
    }
}
