package com.prof18.feedflow.android.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.glance.appwidget.GlanceAppWidgetManager
import com.prof18.feedflow.android.widget.FeedFlowWidget

@Composable
fun SettingsScreen(
    navigateBack: () -> Unit,
    navigateToAppearance: () -> Unit,
    navigateToFeedsAndAccounts: () -> Unit,
    navigateToFeedListSettings: () -> Unit,
    navigateToReadingBehavior: () -> Unit,
    navigateToSyncAndStorage: () -> Unit,
    navigateToWidgetSettings: () -> Unit,
    navigateToAboutAndSupport: () -> Unit,
) {
    val context = LocalContext.current
    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
    val appVersion = packageInfo.versionName ?: ""
    var hasWidget by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val ids = GlanceAppWidgetManager(context).getGlanceIds(FeedFlowWidget::class.java)
        hasWidget = ids.isNotEmpty()
    }

    SettingsScreenContent(
        appVersion = appVersion,
        navigateBack = navigateBack,
        navigateToAppearance = navigateToAppearance,
        navigateToFeedsAndAccounts = navigateToFeedsAndAccounts,
        navigateToFeedListSettings = navigateToFeedListSettings,
        navigateToReadingBehavior = navigateToReadingBehavior,
        navigateToSyncAndStorage = navigateToSyncAndStorage,
        navigateToWidgetSettings = navigateToWidgetSettings,
        navigateToAboutAndSupport = navigateToAboutAndSupport,
        showWidgetSettings = hasWidget,
    )
}
