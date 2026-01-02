package com.prof18.feedflow.android.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prof18.feedflow.android.base.BaseThemeActivity
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import org.koin.androidx.viewmodel.ext.android.viewModel

class WidgetConfigurationActivity : BaseThemeActivity() {

    private val viewModel: WidgetConfigurationViewModel by viewModel()

    private var appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID
    private lateinit var resultValue: Intent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        resultValue = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        setResult(RESULT_CANCELED, resultValue)

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
    }

    @Composable
    override fun Content() {
        val settingsState by viewModel.settingsState.collectAsStateWithLifecycle()
        val strings = LocalFeedFlowStrings.current

        WidgetSettingsScaffold(
            title = strings.widgetConfigurationTitle,
            settingsState = settingsState,
            onSyncPeriodSelected = viewModel::updateSyncPeriod,
            onFeedLayoutSelected = viewModel::updateFeedLayout,
            onShowHeaderSelected = viewModel::updateShowHeader,
            onFontScaleSelected = viewModel::updateFontScale,
            onBackgroundColorSelected = viewModel::updateBackgroundColor,
            onBackgroundOpacitySelected = viewModel::updateBackgroundOpacityPercent,
            showConfirmButton = true,
            onConfirm = {
                viewModel.enqueueWorker()
                setResult(RESULT_OK, resultValue)
                finish()
            },
        )
    }
}
