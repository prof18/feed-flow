package com.prof18.feedflow

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import com.prof18.feedflow.domain.feed.manager.FeedManagerRepository
import com.prof18.feedflow.domain.model.Browser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class BrowserManager(
    private val context: Context,
    private val feedManagerRepository: FeedManagerRepository,
) {

    private val browserListMutableState = MutableStateFlow<List<Browser>>(emptyList())
    val browserListState = browserListMutableState.asStateFlow()

    init {
        populateBrowserList()
    }

    fun getBrowserPackageName(): String? {
        return browserListMutableState.value.firstOrNull { it.isFavourite }?.id
    }

    fun setFavouriteBrowser(browser: Browser) {
        feedManagerRepository.setFavouriteBrowser(browser)
        browserListMutableState.update { browserList ->
            val newList = browserList.toMutableList()
            newList.replaceAll {
                it.copy(isFavourite = it.id == browser.id)
            }
            newList
        }
    }

    private fun populateBrowserList() {
        val favouriteBrowserId = feedManagerRepository.getFavouriteBrowserId()

        val intent = Intent(Intent(Intent.ACTION_VIEW)).apply {
            data = Uri.parse("https://www.example.com")
        }

        val resolvedInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.queryIntentActivities(
                intent,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.queryIntentActivities(
                Intent(Intent.ACTION_MAIN, null).addCategory(Intent.CATEGORY_LAUNCHER),
                PackageManager.GET_META_DATA
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
        }
        browserListMutableState.update { browserList }
    }

    fun openUrl(
        url: String,
        context: Context,
    ) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(url)
            getBrowserPackageName()?.let { packageName ->
                setPackage(packageName)
            }
        }
        context.startActivity(intent)
    }
}
