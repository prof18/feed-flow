package com.prof18.feedflow.android

import android.content.Context
import android.content.Intent

internal fun Context.openShareSheet(title: String?, url: String) {
    val subject = if (!title.isNullOrBlank()) title else url
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, url)
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TITLE, subject)
        type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, null)
    startActivity(shareIntent)
}
