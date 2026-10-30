package com.prof18.feedflow.android.widget

import android.content.Context
import android.content.res.ColorStateList
import android.os.Build
import android.view.View
import android.widget.RemoteViews
import androidx.core.graphics.drawable.toBitmap
import com.prof18.feedflow.android.R

// Stable view IDs let the action show loading with a partial update before a full Glance render finishes.
internal fun widgetRefreshButtonViews(
    context: Context,
    textColor: Int,
    contentDescription: String,
    isRefreshing: Boolean,
): RemoteViews = widgetRefreshButtonFeedback(context, isRefreshing).apply {
    val icon = requireNotNull(context.getDrawable(R.drawable.ic_widget_refresh))
    icon.setTint(textColor)
    setImageViewBitmap(R.id.widget_refresh_icon, icon.toBitmap())
    setContentDescription(R.id.widget_refresh_icon, contentDescription)
    setContentDescription(R.id.widget_refresh_progress, contentDescription)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        setColorStateList(
            R.id.widget_refresh_progress,
            "setIndeterminateTintList",
            ColorStateList.valueOf(textColor),
        )
    }
}

internal fun widgetRefreshButtonFeedback(context: Context, isRefreshing: Boolean): RemoteViews =
    RemoteViews(context.packageName, R.layout.widget_refresh_button).apply {
        setViewVisibility(R.id.widget_refresh_icon, if (isRefreshing) View.GONE else View.VISIBLE)
        setViewVisibility(R.id.widget_refresh_progress, if (isRefreshing) View.VISIBLE else View.GONE)
    }
