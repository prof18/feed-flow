package com.prof18.feedflow.android

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.browser.customtabs.CustomTabsIntent
import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.utils.BrowserIds
import com.prof18.feedflow.i18n.EnFeedFlowStrings
import com.prof18.feedflow.i18n.FeedFlowStrings
import com.prof18.feedflow.i18n.feedFlowStrings
import com.prof18.feedflow.shared.domain.browser.BrowserSettingsRepository
import com.prof18.feedflow.shared.domain.model.Browser
import com.prof18.feedflow.shared.domain.settings.SettingsRepository
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.Locale

class BrowserManager(
    private val context: Context,
    private val browserSettingsRepository: BrowserSettingsRepository,
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
        browserSettingsRepository.setFavouriteBrowser(browser)
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
        val favouriteBrowserId = browserSettingsRepository.getFavouriteBrowserId()

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

    fun openUrlWithFavoriteBrowser(
        url: String,
        context: Context,
    ) {
        try {
            val browserId = getBrowserPackageName()
            if (browserId == BrowserIds.IN_APP_BROWSER) {
                val intent = CustomTabsIntent.Builder()
                    .build()
                intent.launchUrl(context, Uri.parse(url))
            } else {
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

    private fun getFavouriteBrowserIntent(url: String): Intent {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(url)
            getBrowserPackageName()?.let { packageName ->
                setPackage(packageName)
            }
        }
        return intent
    }

    fun openUrlWithDefaultBrowser(
        url: String,
        context: Context,
    ) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(url)
            }
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            logger.e(e) { "Unable to start web browser" }
        }
    }
}
