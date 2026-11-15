package com.prof18.feedflow.desktop.settings

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.performClick
import com.prof18.feedflow.core.model.AutoDeletePeriod
import com.prof18.feedflow.core.model.ThemeMode
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.desktop.ui.components.FeedFlowTheme
import com.prof18.feedflow.i18n.EnFeedFlowStrings
import kotlinx.coroutines.flow.MutableSharedFlow
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertTrue

class SettingsUiTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `general pane exposes switch and routes preference change`() {
        val reduceMotion = mutableStateOf(false)
        val forceEnglish = mutableStateOf(false)
        rule.setContent {
            CompositionLocalProvider(LocalFeedFlowStrings provides EnFeedFlowStrings) {
                FeedFlowTheme(darkTheme = false, useOledTheme = false) {
                    GeneralPane(
                        themeMode = ThemeMode.SYSTEM,
                        onThemeModeSelected = {},
                        isMultiPaneEnabled = false,
                        onMultiPaneToggled = {},
                        isReduceMotionEnabled = reduceMotion.value,
                        onReduceMotionToggled = { reduceMotion.value = it },
                        isForceEnglishEnabled = forceEnglish.value,
                        onForceEnglishToggled = { forceEnglish.value = it },
                        isHideUnreadCountEnabled = false,
                        onHideUnreadCountToggled = {},
                    )
                }
            }
        }

        val reduceMotionCheckbox = rule.onNode(
            hasText(EnFeedFlowStrings.settingsReduceMotion) and
                SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch),
        )
        reduceMotionCheckbox.assertExists()
        reduceMotionCheckbox.performClick()

        rule.runOnIdle { assertTrue(reduceMotion.value) }

        rule.onNode(
            hasText(EnFeedFlowStrings.settingsForceEnglish) and
                SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch),
        ).performClick()

        rule.runOnIdle { assertTrue(forceEnglish.value) }
    }

    @Test
    fun `storage pane confirms deleted offline copies`() {
        val events = MutableSharedFlow<Boolean>(extraBufferCapacity = 1)
        rule.setContent {
            CompositionLocalProvider(LocalFeedFlowStrings provides EnFeedFlowStrings) {
                FeedFlowTheme(darkTheme = false, useOledTheme = false) {
                    StoragePane(
                        autoDeletePeriod = AutoDeletePeriod.DISABLED,
                        onAutoDeletePeriodSelected = {},
                        onClearDownloadedArticles = {},
                        downloadedArticlesCleared = events,
                    )
                }
            }
        }

        rule.onNode(hasText(EnFeedFlowStrings.settingsClearDataTitle)).assertExists()
        rule.runOnIdle { events.tryEmit(true) }

        rule.waitUntil(timeoutMillis = 5_000) {
            rule.onAllNodes(hasText(EnFeedFlowStrings.settingsOfflineCopiesDeleted)).fetchSemanticsNodes().isNotEmpty()
        }
    }
}
