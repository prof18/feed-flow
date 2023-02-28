package com.prof18.feedflow

import com.arkivanov.essenty.parcelable.Parcelable

sealed class Screen: Parcelable {

    object Home : Screen()

    object Settings : Screen()

    object ImportFeed: Screen("import_feed")
    object Settings: Screen("setting")
    object AddFeed: Screen("add_feed")
    object FeedList: Screen("feed_list")
}

