package com.prof18.feedflow.desktop.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.core.model.CategoryNameValidationResult
import com.prof18.feedflow.desktop.feedforms.CategoryNameDialog
import com.prof18.feedflow.desktop.feedforms.DeleteFeedConfirmation
import com.prof18.feedflow.desktop.testing.FeedFormE2eIds
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.i18n.EnFeedFlowStrings
import org.junit.Rule
import org.junit.Test

/**
 * The alert-shaped dialogs are sized to their content, so their actions must stay reachable when the
 * body grows: a long message or a large font scale has to scroll the body, never push the buttons out.
 */
class DesktopDialogFitTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `confirmation actions stay visible when a long message is enlarged`() {
        rule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalFeedFlowStrings provides EnFeedFlowStrings,
                LocalDensity provides Density(density.density, fontScale = 2f),
            ) {
                FeedFlowTheme(darkTheme = false, useOledTheme = false) {
                    // The content area the 400x250 window leaves below the macOS title strip.
                    Box(Modifier.size(width = 400.dp, height = 220.dp)) {
                        ConfirmationContent(
                            title = EnFeedFlowStrings.desktopDeleteSelectedFeedsTitle,
                            message = EnFeedFlowStrings.desktopDeleteSelectedFeedsMessage(12).repeat(4),
                            onConfirm = {},
                            onDismiss = {},
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }

        rule.onNodeWithText(EnFeedFlowStrings.cancelButton).assertIsDisplayed()
        rule.onNodeWithText(EnFeedFlowStrings.confirmButton).assertIsDisplayed()
    }

    @Test
    fun `the category name dialog keeps its field and actions visible`() {
        rule.setContent {
            CompositionLocalProvider(LocalFeedFlowStrings provides EnFeedFlowStrings) {
                FeedFlowTheme(darkTheme = false, useOledTheme = false) {
                    CategoryNameDialog(
                        visible = true,
                        title = EnFeedFlowStrings.addFeedCategoryTitle,
                        initialName = "",
                        categoryId = null,
                        validateCategoryName = { _, _ -> CategoryNameValidationResult.VALID },
                        onDismiss = {},
                        onSave = {},
                    )
                }
            }
        }

        rule.onNodeWithTag(FeedFormE2eIds.CATEGORY_ADD_INPUT).assertIsDisplayed()
        rule.onNodeWithTag(FeedFormE2eIds.CATEGORY_ADD_CONFIRM).assertIsDisplayed()
        rule.onNodeWithText(EnFeedFlowStrings.cancelButton).assertIsDisplayed()
    }

    @Test
    fun `the delete feed confirmation keeps its message and actions visible`() {
        rule.setContent {
            CompositionLocalProvider(LocalFeedFlowStrings provides EnFeedFlowStrings) {
                FeedFlowTheme(darkTheme = false, useOledTheme = false) {
                    DeleteFeedConfirmation(visible = true, onDismiss = {}, onConfirm = {})
                }
            }
        }

        rule.onNodeWithText(EnFeedFlowStrings.deleteFeedConfirmationMessage).assertIsDisplayed()
        rule.onNodeWithText(EnFeedFlowStrings.deleteCategoryCloseButton).assertIsDisplayed()
        rule.onNodeWithText(EnFeedFlowStrings.deleteFeed).assertIsDisplayed()
    }
}
