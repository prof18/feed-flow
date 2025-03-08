package com.prof18.feedflow.android.widget

import FeedFlowTheme
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prof18.feedflow.android.settings.components.SyncPeriodDialog
import com.prof18.feedflow.shared.domain.model.SyncPeriod
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import com.prof18.feedflow.shared.ui.utils.ProvideFeedFlowStrings
import com.prof18.feedflow.shared.ui.utils.rememberFeedFlowStrings
import org.koin.androidx.viewmodel.ext.android.viewModel

class WidgetConfigurationActivity : ComponentActivity() {

    private val viewModel: WidgetConfigurationViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        val appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        val resultValue = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        setResult(RESULT_CANCELED, resultValue)

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setContent {
            FeedFlowTheme {
                val lyricist = rememberFeedFlowStrings()
                ProvideFeedFlowStrings(lyricist) {
                    val syncPeriod by viewModel.syncPeriodState.collectAsStateWithLifecycle()
                    WidgetConfigurationScreen(
                        syncPeriod = syncPeriod,
                        onSyncPeriodSelected = viewModel::updateSyncPeriod,
                        onConfirm = {
                            viewModel.enqueueWorker()
                            setResult(RESULT_OK, resultValue)
                            finish()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun WidgetConfigurationScreen(
    syncPeriod: SyncPeriod,
    onSyncPeriodSelected: (SyncPeriod) -> Unit,
    onConfirm: () -> Unit,
) {
    var showSyncPeriodDialog by remember { mutableStateOf(false) }
    val strings = LocalFeedFlowStrings.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = strings.widgetConfigurationTitle) },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = Spacing.regular),
        ) {
            Text(
                text = strings.widgetConfigurationDescription,
                style = MaterialTheme.typography.bodyLarge,
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable { showSyncPeriodDialog = true }
                    .fillMaxWidth()
                    .padding(vertical = Spacing.xsmall)
                    .padding(top = Spacing.regular),
                horizontalArrangement = Arrangement.spacedBy(Spacing.regular),
            ) {
                Icon(
                    Icons.Outlined.Sync,
                    contentDescription = null,
                )

                Column(
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = strings.settingsSyncPeriod,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = when (syncPeriod) {
                            SyncPeriod.ONE_HOUR -> strings.settingsSyncPeriodOneHour
                            SyncPeriod.TWO_HOURS -> strings.settingsSyncPeriodTwoHours
                            SyncPeriod.SIX_HOURS -> strings.settingsSyncPeriodSixHours
                            SyncPeriod.TWELVE_HOURS -> strings.settingsSyncPeriodTwelveHours
                            SyncPeriod.NEVER -> "" // This is never shown here
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Button(
                onClick = onConfirm,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Spacing.medium),
            ) {
                Text(text = strings.widgetConfigurationConfirm)
            }
        }

        if (showSyncPeriodDialog) {
            SyncPeriodDialog(
                currentPeriod = syncPeriod,
                showNeverSync = false,
                onPeriodSelected = onSyncPeriodSelected,
                dismissDialog = { showSyncPeriodDialog = false },
            )
        }
    }
}
