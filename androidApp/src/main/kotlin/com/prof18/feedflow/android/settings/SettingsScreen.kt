package com.prof18.feedflow.android.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.glance.appwidget.GlanceAppWidgetManager
import com.prof18.feedflow.android.billing.RevenueCatSupport
import com.prof18.feedflow.android.widget.FeedFlowWidget

@Composable
fun SettingsScreen(
    navigateBack: () -> Unit,
    navigateToFeeds: () -> Unit,
    navigateToAddFeed: () -> Unit,
    navigateToImportExport: () -> Unit,
    navigateToBlockedWords: () -> Unit,
    navigateToSyncAndRefresh: () -> Unit,
    navigateToNotifications: () -> Unit,
    navigateToStorage: () -> Unit,
    navigateToAppearance: () -> Unit,
    navigateToFeedListSettings: () -> Unit,
    navigateToReadingBehavior: () -> Unit,
    navigateToWidgetSettings: () -> Unit,
    navigateToAboutAndSupport: () -> Unit,
    navigateToSupportPaywall: () -> Unit,
) {
    val context = LocalContext.current
    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
    val appVersion = packageInfo.versionName.orEmpty()
    var hasWidget by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        val ids = GlanceAppWidgetManager(context).getGlanceIds(FeedFlowWidget::class.java)
        hasWidget = ids.isNotEmpty()
    }

    SettingsScreenContent(
        appVersion = appVersion,
        navigateBack = navigateBack,
        navigateToFeeds = navigateToFeeds,
        navigateToAddFeed = navigateToAddFeed,
        navigateToImportExport = navigateToImportExport,
        navigateToBlockedWords = navigateToBlockedWords,
        navigateToSyncAndRefresh = navigateToSyncAndRefresh,
        navigateToNotifications = navigateToNotifications,
        navigateToStorage = navigateToStorage,
        navigateToAppearance = navigateToAppearance,
        navigateToFeedListSettings = navigateToFeedListSettings,
        navigateToReadingBehavior = navigateToReadingBehavior,
        navigateToWidgetSettings = navigateToWidgetSettings,
        navigateToAboutAndSupport = navigateToAboutAndSupport,
        onSupportProjectClick = navigateToSupportPaywall,
        showWidgetSettings = hasWidget,
        showSupportProject = RevenueCatSupport.isAvailable,
    )
}
