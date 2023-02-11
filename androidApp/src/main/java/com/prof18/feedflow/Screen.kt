package com.prof18.feedflow

sealed class Screen(val name: String) {
    object Home: Screen("home")
    object ImportFeed: Screen("import_feed")
}