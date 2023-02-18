package com.prof18.feedflow

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build

object BrowserSelector {

    private var defaultBrowserPackageName: String? = null

    fun getBrowserPackageName(context: Context, intent: Intent): String {
        return if (defaultBrowserPackageName != null) {
            defaultBrowserPackageName!!
        } else {
            val packageName = retrieveBrowserPackageName(context, intent)
            defaultBrowserPackageName = packageName
            packageName
        }
    }

    private fun retrieveBrowserPackageName(context: Context, intent: Intent): String {
        val resolvedInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.queryIntentActivities(
                intent,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong())
            )
        } else {
            context.packageManager.queryIntentActivities(
                Intent(Intent.ACTION_MAIN, null).addCategory(Intent.CATEGORY_LAUNCHER),
                PackageManager.GET_META_DATA
            )
        }

        return resolvedInfo.first { it.activityInfo.packageName == "org.mozilla.focus" }
            .activityInfo.packageName
    }

}