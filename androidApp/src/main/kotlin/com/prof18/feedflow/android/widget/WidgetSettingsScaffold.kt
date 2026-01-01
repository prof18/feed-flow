package com.prof18.feedflow.android.widget

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import com.prof18.feedflow.core.model.FeedLayout
import com.prof18.feedflow.shared.domain.model.SyncPeriod
import com.prof18.feedflow.shared.ui.theme.FeedFlowTheme
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings

@Composable
fun WidgetSettingsScaffold(
    title: String,
    syncPeriod: SyncPeriod,
    feedLayout: FeedLayout,
    headerText: String?,
    onSyncPeriodSelected: (SyncPeriod) -> Unit,
    onFeedLayoutSelected: (FeedLayout) -> Unit,
    showConfirmButton: Boolean,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    onNavigateBack: (() -> Unit)? = null,
) {
    Scaffold(
        modifier = modifier,
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
    ) { paddingValues ->
        val layoutDir = LocalLayoutDirection.current
        Column(
            modifier = Modifier
                .padding(top = paddingValues.calculateTopPadding())
                .padding(start = paddingValues.calculateLeftPadding(layoutDir))
                .padding(end = paddingValues.calculateRightPadding(layoutDir)),
        ) {
            WidgetPreviewSection(
                feedLayout = feedLayout,
                headerText = headerText,
            )

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
            ) {
                item {
                    WidgetSettingsContent(
                        syncPeriod = syncPeriod,
                        feedLayout = feedLayout,
                        onSyncPeriodSelected = onSyncPeriodSelected,
                        onFeedLayoutSelected = onFeedLayoutSelected,
                        showConfirmButton = showConfirmButton,
                        onConfirm = onConfirm,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(paddingValues.calculateBottomPadding()))
                }
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
            syncPeriod = SyncPeriod.ONE_HOUR,
            feedLayout = FeedLayout.CARD,
            headerText = strings.widgetSettingsHeader,
            onSyncPeriodSelected = {},
            onFeedLayoutSelected = {},
            showConfirmButton = true,
            onConfirm = {},
            onNavigateBack = {},
        )
    }
}
