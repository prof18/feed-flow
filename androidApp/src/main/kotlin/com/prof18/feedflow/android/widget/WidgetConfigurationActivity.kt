package com.prof18.feedflow.android.widget

import FeedFlowTheme
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prof18.feedflow.android.settings.components.SyncPeriodSelector
import com.prof18.feedflow.core.model.FeedLayout
import com.prof18.feedflow.shared.domain.model.SyncPeriod
import com.prof18.feedflow.shared.ui.settings.FeedLayoutSelector
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
                    val feedLayout by viewModel.feedLayoutState.collectAsStateWithLifecycle()
                    WidgetConfigurationScreen(
                        syncPeriod = syncPeriod,
                        feedLayout = feedLayout,
                        onSyncPeriodSelected = viewModel::updateSyncPeriod,
                        onFeedLayoutSelected = viewModel::updateFeedLayout,
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
    feedLayout: FeedLayout,
    onSyncPeriodSelected: (SyncPeriod) -> Unit,
    onFeedLayoutSelected: (FeedLayout) -> Unit,
    onConfirm: () -> Unit,
) {
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
                .padding(paddingValues),
        ) {
            Text(
                modifier = Modifier.padding(horizontal = Spacing.regular),
                text = strings.widgetConfigurationDescription,
                style = MaterialTheme.typography.bodyLarge,
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
