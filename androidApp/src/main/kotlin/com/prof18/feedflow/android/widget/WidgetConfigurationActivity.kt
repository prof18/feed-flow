package com.prof18.feedflow.android.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prof18.feedflow.android.base.BaseThemeActivity
import com.prof18.feedflow.android.settings.syncstorage.SyncAndStorageScreen
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class WidgetConfigurationActivity : BaseThemeActivity() {

    private val viewModel: WidgetInstanceSettingsViewModel by viewModel {
        parametersOf(appWidgetId)
    }

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
        var showSyncSettings by rememberSaveable { mutableStateOf(false) }
        val stateHolder = rememberSaveableStateHolder()

        BackHandler(enabled = showSyncSettings) {
            showSyncSettings = false
        }
        if (showSyncSettings) {
            SyncAndStorageScreen(navigateBack = { showSyncSettings = false })
            return
        }

        stateHolder.SaveableStateProvider("widget-configuration") {
            WidgetSettingsScaffold(
                title = strings.widgetConfigurationTitle,
                settingsState = settingsState,
                onFeedLayoutSelected = viewModel::updateFeedLayout,
                onShowHeaderSelected = viewModel::updateShowHeader,
                onShowRefreshButtonSelected = viewModel::updateShowRefreshButton,
                onFontScaleSelected = viewModel::updateFontScale,
                onBackgroundColorSelected = viewModel::updateBackgroundColor,
                onBackgroundOpacitySelected = viewModel::updateBackgroundOpacityPercent,
                onTextColorModeSelected = viewModel::updateTextColorMode,
                onHideImagesSelected = viewModel::updateHideImages,
                onContentFilterSelected = viewModel::updateContentFilter,
                onManageSync = { showSyncSettings = true },
                showConfirmButton = true,
                onConfirm = {
                    setResult(RESULT_OK, resultValue)
                    finish()
                },
            )
        }
    }
}
