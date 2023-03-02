package com.prof18.feedflow.navigation

import com.arkivanov.essenty.parcelable.Parcelable

sealed class Screen: Parcelable {
    object Home : Screen()
    object Settings : Screen()
    object ImportFeed: Screen()
    object AddFeed: Screen()
    object FeedList: Screen()
}
