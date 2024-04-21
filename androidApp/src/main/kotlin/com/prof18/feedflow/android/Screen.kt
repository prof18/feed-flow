package com.prof18.feedflow.android

sealed class Screen(val name: String) {
    data object Home : Screen("home")
    data object Settings : Screen("setting")
    data object AddFeed : Screen("add_feed")
    data object FeedList : Screen("feed_list")
    data object About : Screen("about")
    data object Licenses : Screen("licenses")
    data object ImportExport : Screen("import_export")
    data object ReaderMode : Screen("reader_mode")
}
