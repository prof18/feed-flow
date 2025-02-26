package com.prof18.feedflow.android

import android.content.Context
import android.content.Intent

internal fun Context.openShareSheet(title: String?, url: String) {
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, url)
        title?.let { putExtra(Intent.EXTRA_SUBJECT, it) }
        type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, null)
    startActivity(shareIntent)
}
