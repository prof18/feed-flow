package com.prof18.feedflow.desktop.navigation

import com.arkivanov.essenty.parcelable.Parcelable

sealed class Screen : Parcelable {
    data object Home : Screen()
    data object FeedList : Screen()

    data object ImportExport : Screen()
}
