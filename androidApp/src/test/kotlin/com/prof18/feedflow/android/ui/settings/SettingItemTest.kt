package com.prof18.feedflow.android.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.prof18.feedflow.android.ui.theme.FeedFlowTheme
import com.prof18.feedflow.android.ui.utils.ProvideFeedFlowStrings
import com.prof18.feedflow.android.ui.utils.rememberFeedFlowStrings
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SettingItemTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun showsSupportingTextWhenGiven() {
        setContent {
            SettingItem(title = "T", supportingText = "S", onClick = {})
        }

        compose.onNodeWithText("S").assertIsDisplayed()
    }

    @Test
    fun sectionHeaderIsAHeading() {
        setContent {
            SettingsSectionHeader(title = "H")
        }

        compose.onNodeWithText("H").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
    }

    private fun setContent(content: @Composable () -> Unit) {
        compose.setContent {
            FeedFlowTheme {
                ProvideFeedFlowStrings(rememberFeedFlowStrings()) { content() }
            }
        }
    }
}
