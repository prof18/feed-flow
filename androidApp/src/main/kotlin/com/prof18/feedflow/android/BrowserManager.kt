package com.prof18.feedflow.android

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import co.touchlab.kermit.Logger
import com.prof18.feedflow.android.ui.utils.feedFlowStringsFor
import com.prof18.feedflow.core.model.ArticleOpenMode
import com.prof18.feedflow.core.model.Browser
import com.prof18.feedflow.core.utils.BrowserIds
import com.prof18.feedflow.core.utils.BrowserResolver
import com.prof18.feedflow.i18n.FeedFlowStrings
import com.prof18.feedflow.shared.data.SettingsRepository
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

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

    fun getArticleOpenMode(): ArticleOpenMode =
        settingsRepository.getArticleOpenMode()

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

    private fun getCurrentStrings(): FeedFlowStrings =
        feedFlowStringsFor(forceEnglish = settingsRepository.getForceEnglishEnabled())

    private fun populateBrowserList() {
        val favouriteBrowserId = settingsRepository.getFavouriteBrowserId()

        val intent = Intent(Intent.ACTION_VIEW, "https://www.example.com".toUri())
            .addCategory(Intent.CATEGORY_BROWSABLE)
        // Include browsers other than the system default, but require an activity that can launch.
        val flags = PackageManager.MATCH_ALL or PackageManager.MATCH_DEFAULT_ONLY

        val resolvedInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.queryIntentActivities(
                intent,
                PackageManager.ResolveInfoFlags.of(flags.toLong()),
            )
        } else {
            context.packageManager.queryIntentActivities(
                intent,
                flags,
            )
        }
        val strings = getCurrentStrings()
        val browsers = listOf(
            BrowserIds.IN_APP_BROWSER to strings.inAppBrowser,
            BrowserIds.SYSTEM_DEFAULT_BROWSER to strings.defaultBrowser,
        ) + resolvedInfos.map { info ->
            info.activityInfo.packageName to info.activityInfo.loadLabel(context.packageManager).toString()
        }
        val selectedBrowserId = BrowserResolver.linksBrowserId(favouriteBrowserId, browsers.map { it.first })

        val browserList = browsers.map { (id, name) ->
            Browser(
                id = id,
                name = name,
                isFavourite = selectedBrowserId == id,
            )
        }
        browserListMutableState.update { browserList.toImmutableList() }
    }

    private fun linksBrowserId(): String =
        browserListMutableState.value.firstOrNull { it.isFavourite }?.id ?: BrowserIds.IN_APP_BROWSER

    fun resolveArticleBrowserId(storedArticleBrowserId: String?): String =
        BrowserResolver.articleBrowserId(
            articleBrowserId = storedArticleBrowserId,
            favouriteId = settingsRepository.getFavouriteBrowserId(),
            availableIds = browserListMutableState.value.map { it.id },
        )

    fun articleBrowserId(): String =
        resolveArticleBrowserId(settingsRepository.getArticleBrowserId())

    fun getArticleBrowserPackageName(): String? =
        articleBrowserId().takeIf {
            it != BrowserIds.IN_APP_BROWSER && it != BrowserIds.SYSTEM_DEFAULT_BROWSER
        }

    fun openUrlWithFavoriteBrowser(
        url: String,
        context: Context,
        preferAppHandler: Boolean = true,
    ) {
        openWithBrowserId(linksBrowserId(), url, context, preferAppHandler)
    }

    fun openArticleInBrowser(
        url: String,
        context: Context,
        preferAppHandler: Boolean = true,
    ) {
        openWithBrowserId(articleBrowserId(), url, context, preferAppHandler)
    }

    private fun openWithBrowserId(
        browserId: String,
        url: String,
        context: Context,
        preferAppHandler: Boolean,
    ) {
        try {
            if (browserId == BrowserIds.IN_APP_BROWSER) {
                openWithInAppBrowser(url, context)
                return
            }

            val appHandlerOpened = preferAppHandler && tryOpenWithAppHandler(url, context)
            if (!appHandlerOpened) {
                val intent = getBrowserIntent(browserId, url)
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
        val uri = url.toUri()
        val intent = CustomTabsIntent.Builder().build()
        getInAppBrowserPackage(uri)?.let { packageName ->
            intent.intent.setPackage(packageName)
        }
        intent.launchUrl(context, uri)
    }

    // Without a default browser, an unpinned Custom Tab opens the system "Open with" chooser.
    private fun getInAppBrowserPackage(uri: Uri): String? {
        val viewIntent = Intent(Intent.ACTION_VIEW, uri).addCategory(Intent.CATEGORY_BROWSABLE)
        val packageManager = context.packageManager
        return inAppBrowserPackage(
            resolvedPackage = packageManager.resolveActivity(viewIntent, PackageManager.MATCH_DEFAULT_ONLY)
                ?.activityInfo?.packageName,
            handlerPackages = packageManager.queryIntentActivities(viewIntent, PackageManager.MATCH_DEFAULT_ONLY)
                .map { it.activityInfo.packageName },
            customTabsProvider = { candidates -> CustomTabsClient.getPackageName(context, candidates) },
        )
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

    private fun getBrowserIntent(browserId: String, url: String): Intent {
        return Intent(Intent.ACTION_VIEW).apply {
            data = url.toUri()
            if (browserId != BrowserIds.SYSTEM_DEFAULT_BROWSER) {
                setPackage(browserId)
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

/**
 * Returns the Custom Tabs provider to pin when the system has no default handler for the link and
 * would show a chooser, or null to leave routing (default browser, verified app links) to the system.
 */
internal fun inAppBrowserPackage(
    resolvedPackage: String?,
    handlerPackages: List<String>,
    customTabsProvider: (candidates: List<String>) -> String?,
): String? {
    val hasDefaultHandler = resolvedPackage != null && resolvedPackage in handlerPackages
    return if (hasDefaultHandler) null else customTabsProvider(handlerPackages)
}
