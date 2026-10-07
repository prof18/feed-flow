package com.prof18.feedflow.android.widget

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import com.prof18.feedflow.core.model.WidgetContentFilter
import com.prof18.feedflow.core.model.WidgetFeedLayout
import com.prof18.feedflow.shared.domain.model.SyncPeriod
import com.prof18.feedflow.shared.domain.model.WidgetTextColorMode
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.theme.FeedFlowTheme
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import com.prof18.feedflow.shared.ui.utils.exposeTestTagsAsResourceIds

@Composable
fun WidgetSettingsScaffold(
    title: String,
    settingsState: WidgetSettingsState,
    onFeedLayoutSelected: (WidgetFeedLayout) -> Unit,
    onShowHeaderSelected: (Boolean) -> Unit,
    onShowRefreshButtonSelected: (Boolean) -> Unit,
    onFontScaleSelected: (Int) -> Unit,
    onBackgroundColorSelected: (Int?) -> Unit,
    onBackgroundOpacitySelected: (Int) -> Unit,
    onTextColorModeSelected: (WidgetTextColorMode) -> Unit,
    onHideImagesSelected: (Boolean) -> Unit,
    onContentFilterSelected: (WidgetContentFilter) -> Unit,
    onManageSync: () -> Unit,
    showConfirmButton: Boolean,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    onNavigateBack: (() -> Unit)? = null,
) {
    val strings = LocalFeedFlowStrings.current
    Scaffold(
        modifier = modifier.exposeTestTagsAsResourceIds(),
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Default.ArrowBack,
                                contentDescription = null,
                            )
                        }
                    }
                },
            )
        },
        bottomBar = {
            if (showConfirmButton) {
                Surface(modifier = Modifier.navigationBarsPadding()) {
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.regular)
                            .testTag("widget_configuration_done"),
                    ) {
                        Text(strings.actionDone)
                    }
                }
            }
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
        ) {
            item {
                WidgetSettingsContent(
                    settingsState = settingsState,
                    onFeedLayoutSelected = onFeedLayoutSelected,
                    onShowHeaderSelected = onShowHeaderSelected,
                    onShowRefreshButtonSelected = onShowRefreshButtonSelected,
                    onFontScaleSelected = onFontScaleSelected,
                    onBackgroundColorSelected = onBackgroundColorSelected,
                    onBackgroundOpacitySelected = onBackgroundOpacitySelected,
                    onTextColorModeSelected = onTextColorModeSelected,
                    onHideImagesSelected = onHideImagesSelected,
                    onContentFilterSelected = onContentFilterSelected,
                    onManageSync = onManageSync,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Preview
@Composable
private fun WidgetSettingsScaffoldPreview() {
    val strings = LocalFeedFlowStrings.current
    FeedFlowTheme {
        WidgetSettingsScaffold(
            title = strings.widgetConfigurationTitle,
            settingsState = WidgetSettingsState(
                syncPeriod = SyncPeriod.ONE_HOUR,
                feedLayout = WidgetFeedLayout.CARD,
                showHeader = true,
                fontScale = 0,
                backgroundColor = null,
                backgroundOpacityPercent = 100,
            ),
            onFeedLayoutSelected = {},
            onShowHeaderSelected = {},
            onShowRefreshButtonSelected = {},
            onFontScaleSelected = {},
            onBackgroundColorSelected = {},
            onBackgroundOpacitySelected = {},
            onTextColorModeSelected = {},
            onHideImagesSelected = {},
            onContentFilterSelected = {},
            onManageSync = {},
            showConfirmButton = true,
            onConfirm = {},
            onNavigateBack = {},
        )
    }
}
