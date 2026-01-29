package com.prof18.feedflow.android.settings.readingbehavior

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.automirrored.outlined.PlaylistAddCheck
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.MarkAsUnread
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import com.prof18.feedflow.android.settings.components.BrowserSelector
import com.prof18.feedflow.shared.domain.model.Browser
import com.prof18.feedflow.shared.presentation.model.ReadingBehaviorState
import com.prof18.feedflow.shared.ui.settings.ConfirmationDialogConfig
import com.prof18.feedflow.shared.ui.settings.SettingSwitchItem
import com.prof18.feedflow.shared.ui.theme.FeedFlowTheme
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun ReadingBehaviorScreenContent(
    navigateBack: () -> Unit,
    state: ReadingBehaviorState,
    browsers: ImmutableList<Browser>,
    onBrowserSelected: (Browser) -> Unit,
    setReaderMode: (Boolean) -> Unit,
    setSaveReaderModeContent: (Boolean) -> Unit,
    setPrefetchArticleContent: (Boolean) -> Unit,
    setMarkReadWhenScrolling: (Boolean) -> Unit,
    setShowReadItem: (Boolean) -> Unit,
    setHideReadItems: (Boolean) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(LocalFeedFlowStrings.current.settingsReadingBehavior) },
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = null,
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        val layoutDir = LocalLayoutDirection.current
        LazyColumn(
            modifier = Modifier
                .padding(top = paddingValues.calculateTopPadding())
                .padding(start = paddingValues.calculateLeftPadding(layoutDir))
                .padding(end = paddingValues.calculateRightPadding(layoutDir)),
        ) {
            item {
                BrowserSelector(
                    browsers = browsers,
                    onBrowserSelected = onBrowserSelected,
                )
            }

            item {
                SettingSwitchItem(
                    title = LocalFeedFlowStrings.current.settingsReaderMode,
                    icon = Icons.AutoMirrored.Outlined.Article,
                    isChecked = state.isReaderModeEnabled,
                    onCheckedChange = setReaderMode,
                )
            }

            item {
                SettingSwitchItem(
                    title = LocalFeedFlowStrings.current.settingsSaveReaderModeContent,
                    icon = Icons.AutoMirrored.Outlined.Article,
                    isChecked = state.isSaveReaderModeContentEnabled,
                    onCheckedChange = setSaveReaderModeContent,
                )
            }

            item {
                SettingSwitchItem(
                    title = LocalFeedFlowStrings.current.settingsPrefetchArticleContent,
                    icon = Icons.Outlined.CloudDownload,
                    isChecked = state.isPrefetchArticleContentEnabled,
                    onCheckedChange = setPrefetchArticleContent,
                    confirmationDialog = ConfirmationDialogConfig(
                        title = LocalFeedFlowStrings.current.settingsPrefetchArticleContent,
                        message = LocalFeedFlowStrings.current.settingsPrefetchArticleContentWarning,
                    ),
                )
            }

            item {
                SettingSwitchItem(
                    title = LocalFeedFlowStrings.current.toggleMarkReadWhenScrolling,
                    icon = Icons.Outlined.MarkAsUnread,
                    isChecked = state.isMarkReadWhenScrollingEnabled,
                    onCheckedChange = setMarkReadWhenScrolling,
                )
            }

            item {
                SettingSwitchItem(
                    title = LocalFeedFlowStrings.current.settingsToggleShowReadArticles,
                    icon = Icons.AutoMirrored.Outlined.PlaylistAddCheck,
                    isChecked = state.isShowReadItemsEnabled,
                    onCheckedChange = setShowReadItem,
                )
            }

            item {
                SettingSwitchItem(
                    title = LocalFeedFlowStrings.current.settingsHideReadItems,
                    icon = Icons.Outlined.MarkAsUnread,
                    isChecked = state.isHideReadItemsEnabled,
                    onCheckedChange = setHideReadItems,
                )
            }

            item {
                Spacer(modifier = Modifier.height(paddingValues.calculateBottomPadding()))
            }
        }
    }
}

@Preview
@Composable
private fun ReadingBehaviorScreenContentPreview() {
    FeedFlowTheme {
        ReadingBehaviorScreenContent(
            navigateBack = {},
            state = ReadingBehaviorState(
                isReaderModeEnabled = true,
                isSaveReaderModeContentEnabled = false,
                isPrefetchArticleContentEnabled = false,
                isMarkReadWhenScrollingEnabled = true,
                isShowReadItemsEnabled = true,
                isHideReadItemsEnabled = false,
            ),
            browsers = persistentListOf(),
            onBrowserSelected = {},
            setReaderMode = {},
            setSaveReaderModeContent = {},
            setPrefetchArticleContent = {},
            setMarkReadWhenScrolling = {},
            setShowReadItem = {},
            setHideReadItems = {},
        )
    }
}
