package com.prof18.feedflow.android.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.prof18.feedflow.android.settings.components.SyncPeriodSelector
import com.prof18.feedflow.core.model.FeedLayout
import com.prof18.feedflow.shared.domain.model.SyncPeriod
import com.prof18.feedflow.shared.ui.settings.FeedLayoutSelector
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.theme.FeedFlowTheme
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings

@Composable
fun WidgetSettingsContent(
    syncPeriod: SyncPeriod,
    feedLayout: FeedLayout,
    onSyncPeriodSelected: (SyncPeriod) -> Unit,
    onFeedLayoutSelected: (FeedLayout) -> Unit,
    showConfirmButton: Boolean,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalFeedFlowStrings.current

    Column(
        modifier = modifier,
    ) {
        Text(
            modifier = Modifier.padding(horizontal = Spacing.regular),
            text = strings.widgetConfigurationDescription,
            style = MaterialTheme.typography.bodyMedium,
        )

        SyncPeriodSelector(
            currentPeriod = syncPeriod,
            onPeriodSelected = onSyncPeriodSelected,
            showNeverSync = false,
        )

        FeedLayoutSelector(
            feedLayout = feedLayout,
            onFeedLayoutSelected = onFeedLayoutSelected,
        )

        if (showConfirmButton) {
            Button(
                onClick = onConfirm,
                modifier = Modifier
                    .padding(horizontal = Spacing.regular)
                    .fillMaxWidth()
                    .padding(vertical = Spacing.medium),
            ) {
                Text(text = strings.widgetConfigurationConfirm)
            }
        }
    }
}

@Preview
@Composable
private fun WidgetSettingsContentPreview() {
    FeedFlowTheme {
        Surface(
            modifier = Modifier.background(MaterialTheme.colorScheme.background),
        ) {
            WidgetSettingsContent(
                syncPeriod = SyncPeriod.ONE_HOUR,
                feedLayout = FeedLayout.CARD,
                onSyncPeriodSelected = {},
                onFeedLayoutSelected = {},
                showConfirmButton = true,
                onConfirm = {},
            )
        }
    }
}
