package com.prof18.feedflow.android

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.utils.BrowserIds
import com.prof18.feedflow.i18n.EnFeedFlowStrings
import com.prof18.feedflow.i18n.FeedFlowStrings
import com.prof18.feedflow.i18n.feedFlowStrings
import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.domain.model.Browser
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.*

class BrowserManager(
    private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val logger: Logger,
) {

    private val browserListMutableState = MutableStateFlow<ImmutableList<Browser>>(
        persistentListOf(),
    )
    val browserListState = browserListMutableState.asStateFlow()

    init {
        populateBrowserList()
    }

    fun openReaderMode(): Boolean =
        settingsRepository.isUseReaderModeEnabled()

    fun setFavouriteBrowser(browser: Browser) {
        settingsRepository.saveFavouriteBrowserId(browser.id)
        browserListMutableState.update { browserList ->
            val newList = browserList.toMutableList()
            newList.replaceAll {
                it.copy(isFavourite = it.id == browser.id)
            }
            newList.toImmutableList()
        }
    }

    private fun getCurrentStrings(): FeedFlowStrings {
        val languageCode = Locale.getDefault().language
        return feedFlowStrings[languageCode] ?: EnFeedFlowStrings
    }

    private fun populateBrowserList() {
        val favouriteBrowserId = settingsRepository.getFavouriteBrowserId()

        val intent = Intent(Intent(Intent.ACTION_VIEW)).apply {
            data = Uri.parse("https://www.example.com")
        }

        val resolvedInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.queryIntentActivities(
                intent,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong()),
            )
        } else {
            context.packageManager.queryIntentActivities(
                Intent(Intent.ACTION_MAIN, null).addCategory(Intent.CATEGORY_LAUNCHER),
                PackageManager.GET_META_DATA,
            )
        }

        val browserList = listOf(
            Browser(
                id = BrowserIds.IN_APP_BROWSER,
                name = getCurrentStrings().inAppBrowser,
                isFavourite = if (favouriteBrowserId != null) {
                    favouriteBrowserId == BrowserIds.IN_APP_BROWSER
                } else {
                    true
                },
            ),
        ) + resolvedInfos.map { info ->
            val id = info.activityInfo.packageName
            Browser(
                id = id,
                name = info.activityInfo.loadLabel(context.packageManager).toString(),
                isFavourite = if (favouriteBrowserId != null) {
                    favouriteBrowserId == id
                } else {
                    false
                },
            )
        }
        browserListMutableState.update { browserList.toImmutableList() }
    }

    private fun getBrowserPackageName(): String? {
        return browserListMutableState.value.firstOrNull { it.isFavourite }?.id
    }

    fun getBrowserPackageNameWithoutInApp(): String? {
        return browserListMutableState.value.firstOrNull { it.isFavourite && it.id != BrowserIds.IN_APP_BROWSER }?.id
    }

    fun openUrlWithFavoriteBrowser(
        url: String,
        context: Context,
    ) {
        try {
            val browserId = getBrowserPackageName()
            if (browserId == BrowserIds.IN_APP_BROWSER) {
                openWithInAppBrowser(url, context)
                return
            }

            val appHandlerOpened = tryOpenWithAppHandler(url, context)
            if (!appHandlerOpened) {
                val intent = getFavouriteBrowserIntent(url)
                context.startActivity(intent)
            }
        } catch (e: ActivityNotFoundException) {
            logger.e(e) {
                "Favourite browser not valid, open with the default one"
            }
            openUrlWithDefaultBrowser(url, context)
        }
    }

    fun openWithInAppBrowser(url: String, context: Context) {
        val intent = CustomTabsIntent.Builder().build()
        intent.launchUrl(context, url.toUri())
    }

    private fun tryOpenWithAppHandler(url: String, context: Context): Boolean {
        val appHandlerIntent = Intent(Intent.ACTION_VIEW).apply {
            data = url.toUri()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                flags = Intent.FLAG_ACTIVITY_REQUIRE_NON_BROWSER
            }
        }

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            tryOpenWithAppHandlerModern(appHandlerIntent, context)
        } else {
            tryOpenWithAppHandlerLegacy(appHandlerIntent, context)
        }
    }

    private fun tryOpenWithAppHandlerModern(intent: Intent, context: Context): Boolean {
        return try {
            context.startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
    }

    private fun tryOpenWithAppHandlerLegacy(intent: Intent, context: Context): Boolean {
        val nonBrowserHandlers = getNonBrowserHandlers(intent)
        return if (nonBrowserHandlers.isNotEmpty()) {
            context.startActivity(intent)
            true
        } else {
            false
        }
    }

    private fun getNonBrowserHandlers(intent: Intent) =
        context.packageManager.queryIntentActivities(
            intent,
            PackageManager.MATCH_DEFAULT_ONLY,
        ).filterNot { resolveInfo ->
            isBrowserApp(resolveInfo.activityInfo.packageName)
        }

    private fun isBrowserApp(packageName: String): Boolean {
        val testIntent = Intent(Intent.ACTION_VIEW).apply {
            data = "https://www.example.com".toUri()
        }
        val browserResolveInfos = context.packageManager.queryIntentActivities(
            testIntent,
            PackageManager.MATCH_DEFAULT_ONLY,
        )
        return browserResolveInfos.any {
            it.activityInfo.packageName == packageName
        }
    }

    private fun getFavouriteBrowserIntent(url: String): Intent {
        return Intent(Intent.ACTION_VIEW).apply {
            data = url.toUri()
            getBrowserPackageName()?.let { packageName ->
                setPackage(packageName)
            }
        }
    }

    fun openUrlWithDefaultBrowser(
        url: String,
        context: Context,
    ) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = url.toUri()
            }
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            logger.e(e) { "Unable to start web browser" }
        }
    }
}
