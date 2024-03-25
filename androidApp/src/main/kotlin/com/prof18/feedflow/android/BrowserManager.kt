package com.prof18.feedflow.android

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import co.touchlab.kermit.Logger
import com.prof18.feedflow.shared.domain.browser.BrowserSettingsRepository
import com.prof18.feedflow.shared.domain.model.Browser
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class BrowserManager(
    private val context: Context,
    private val browserSettingsRepository: BrowserSettingsRepository,
    private val logger: Logger,
) {

    private val browserListMutableState = MutableStateFlow<ImmutableList<Browser>>(persistentListOf())
    val browserListState = browserListMutableState.asStateFlow()

    init {
        populateBrowserList()
    }

    private fun getBrowserPackageName(): String? {
        return browserListMutableState.value.firstOrNull { it.isFavourite }?.id
    }

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

        val browserList = resolvedInfos.mapIndexed { index, info ->
            val id = info.activityInfo.packageName
            Browser(
                id = id,
                name = info.activityInfo.loadLabel(context.packageManager).toString(),
                isFavourite = if (favouriteBrowserId != null) {
                    favouriteBrowserId == id
                } else {
                    index == 0
                },
            )
        }.toImmutableList()
        browserListMutableState.update { browserList }
    }

    fun openUrlWithFavoriteBrowser(
        url: String,
        context: Context,
    ) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(url)
            getBrowserPackageName()?.let { packageName ->
                setPackage(packageName)
            }
        }
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            logger.e(e) {
                "Favourite browser not valid, open with the default one"
            }
            openUrlWithDefaultBrowser(url, context)
        }
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
