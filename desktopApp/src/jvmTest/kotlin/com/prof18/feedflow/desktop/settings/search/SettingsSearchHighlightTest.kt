package com.prof18.feedflow.desktop.settings.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.desktop.settings.SettingsBlock
import com.prof18.feedflow.desktop.settings.SettingsChoice
import com.prof18.feedflow.desktop.settings.SettingsPage
import com.prof18.feedflow.desktop.settings.SettingsSection
import com.prof18.feedflow.desktop.settings.SettingsSwitch
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.desktop.ui.components.FeedFlowTheme
import com.prof18.feedflow.i18n.EnFeedFlowStrings
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.jewel.ui.component.Text
import org.junit.Rule
import org.junit.Test

class SettingsSearchHighlightTest {
    @get:Rule
    val compose = createComposeRule()

    private val jump = mutableStateOf<SettingsSearchJump?>(null)

    @Test
    fun `highlight request scrolls the target row into view`() {
        setPage { SettingsSection { SettingsSwitch(title = "Target", checked = false, onCheckedChange = {}) } }
        compose.onNodeWithText("Target").assertIsNotDisplayed()

        compose.runOnIdle { jump.value = SettingsSearchJump(id = 1, targetTitle = "Target") }

        waitUntilDisplayed("Target")
    }

    @Test
    fun `highlight request scrolls a titled section into view`() {
        setPage { SettingsSection(title = "Card") { SettingsBlock { Text("inner") } } }
        compose.onNodeWithText("inner").assertIsNotDisplayed()

        compose.runOnIdle { jump.value = SettingsSearchJump(id = 1, targetTitle = "Card") }

        waitUntilDisplayed("Card")
        waitUntilDisplayed("inner")
    }

    @Test
    fun `choice row is a highlight target`() {
        setPage {
            SettingsSection {
                SettingsChoice(title = "Pick", choices = persistentListOf(1 to "One"), selected = 1, onSelected = {})
            }
        }
        compose.onNodeWithText("Pick").assertIsNotDisplayed()

        compose.runOnIdle { jump.value = SettingsSearchJump(id = 1, targetTitle = "Pick") }

        waitUntilDisplayed("Pick")
    }

    @Test
    fun `a new jump runs the subpage reset once`() {
        compose.setContent {
            CompositionLocalProvider(LocalSettingsSearchJump provides jump.value) {
                FeedFlowTheme(darkTheme = false, useOledTheme = false) {
                    var sub by rememberSaveable { mutableStateOf(true) }
                    OnSettingsSearchJump { sub = false }
                    Text(if (sub) "sub" else "root")
                }
            }
        }
        compose.onNodeWithText("sub").assertExists()

        compose.runOnIdle { jump.value = SettingsSearchJump(id = 1, targetTitle = null) }

        compose.onNodeWithText("root").assertExists()
    }

    private fun setPage(target: @Composable () -> Unit) {
        compose.setContent {
            CompositionLocalProvider(
                LocalFeedFlowStrings provides EnFeedFlowStrings,
                LocalSettingsSearchJump provides jump.value,
            ) {
                FeedFlowTheme(darkTheme = false, useOledTheme = false) {
                    Box(Modifier.requiredSize(width = 600.dp, height = 400.dp)) {
                        SettingsPage(title = "Page") {
                            Spacer(Modifier.height(3000.dp))
                            target()
                        }
                    }
                }
            }
        }
    }

    private fun waitUntilDisplayed(text: String) {
        compose.waitUntil(timeoutMillis = 2_000) {
            runCatching { compose.onNodeWithText(text).assertIsDisplayed() }.isSuccess
        }
    }
}
