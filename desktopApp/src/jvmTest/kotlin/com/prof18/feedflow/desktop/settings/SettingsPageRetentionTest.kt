package com.prof18.feedflow.desktop.settings

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import com.prof18.feedflow.core.model.FeedImportExportState
import com.prof18.feedflow.core.model.ImportExportContentType
import com.prof18.feedflow.desktop.importexport.ImportExportContent
import com.prof18.feedflow.desktop.settings.blocked.BlockedWordsContent
import com.prof18.feedflow.desktop.settings.blocked.BlockedWordsE2eIds
import com.prof18.feedflow.desktop.settings.blocked.rememberBlockedWordDraft
import com.prof18.feedflow.desktop.testing.ImportExportE2eIds
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.desktop.ui.components.FeedFlowTheme
import com.prof18.feedflow.i18n.EnFeedFlowStrings
import com.prof18.feedflow.shared.presentation.model.BlockedWordAction
import com.prof18.feedflow.shared.presentation.model.BlockedWordOperationState
import com.prof18.feedflow.shared.presentation.model.BlockedWordOperationStatus
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import org.jetbrains.jewel.ui.component.Text
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

class SettingsPageRetentionTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `blocked word completion while away clears only the submitted draft on return`() {
        val operations = mutableStateOf<ImmutableMap<String, BlockedWordOperationState>>(persistentMapOf())
        var adds = 0
        rule.setContent {
            CompositionLocalProvider(LocalFeedFlowStrings provides EnFeedFlowStrings) {
                FeedFlowTheme(darkTheme = false, useOledTheme = false) {
                    val draft = rememberBlockedWordDraft()
                    SettingsContent(DesktopSettingsCategory.FILTERS) { category ->
                        if (category == DesktopSettingsCategory.FILTERS) {
                            BlockedWordsContent(
                                words = persistentListOf(),
                                onAddWord = { word ->
                                    adds++
                                    operations.value = persistentMapOf(
                                        word to BlockedWordOperationState(
                                            generation = 1,
                                            word = word,
                                            action = BlockedWordAction.Add,
                                            status = BlockedWordOperationStatus.Writing,
                                        ),
                                    )
                                },
                                onRemoveWord = {},
                                onCloseRequest = {},
                                operationStates = operations.value,
                                draft = draft,
                                showCloseButton = false,
                            )
                        } else {
                            Text(category.name)
                        }
                    }
                }
            }
        }
        rule.onNodeWithTag(BlockedWordsE2eIds.INPUT).performTextReplacement("fixture")
        rule.onNodeWithTag(BlockedWordsE2eIds.ADD_BUTTON).performClick()
        rule.onNodeWithText(EnFeedFlowStrings.settingsAccounts).performClick()
        rule.runOnIdle {
            operations.value = persistentMapOf(
                "fixture" to operations.value.getValue("fixture").copy(status = BlockedWordOperationStatus.Applied),
            )
        }
        rule.onNodeWithText(EnFeedFlowStrings.settingsFilters).performClick()
        rule.onNodeWithTag(BlockedWordsE2eIds.INPUT).assertTextEquals("")
        rule.runOnIdle { assertEquals(1, adds) }
    }

    @Test
    fun `returning to import export shows completed operation without repeating it`() {
        val operation = mutableStateOf<FeedImportExportState>(
            FeedImportExportState.LoadingExport(ImportExportContentType.FeedsOpml),
        )
        var completions = 0
        rule.setContent {
            CompositionLocalProvider(LocalFeedFlowStrings provides EnFeedFlowStrings) {
                FeedFlowTheme(darkTheme = false, useOledTheme = false) {
                    SettingsContent(DesktopSettingsCategory.SYNC_STORAGE) { category ->
                        if (category == DesktopSettingsCategory.SYNC_STORAGE) {
                            ImportExportContent(
                                feedImportExportState = operation.value,
                                onRetryClick = {},
                                onChooseAnotherFileClick = {},
                                onDoneClick = { completions++ },
                                onImportClick = {},
                                onExportClick = {},
                                onImportArticlesClick = {},
                                onExportArticlesClick = {},
                            )
                        } else {
                            Text(category.name, Modifier.testTag("other-page"))
                        }
                    }
                }
            }
        }
        rule.onNodeWithText(EnFeedFlowStrings.exportStartedMessage).assertExists()
        rule.onNodeWithText(EnFeedFlowStrings.settingsAccounts).performClick()
        rule.runOnIdle { operation.value = FeedImportExportState.ExportSuccess }
        rule.onNodeWithText(EnFeedFlowStrings.settingsSyncAndStorage).performClick()
        rule.onNodeWithTag(ImportExportE2eIds.FEED_EXPORT_SUCCESS).assertExists()
        rule.runOnIdle { assertEquals(0, completions) }
        rule.onNodeWithTag(ImportExportE2eIds.DONE).performClick()
        rule.runOnIdle { assertEquals(1, completions) }
    }
}
