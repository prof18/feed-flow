package com.prof18.feedflow.navigation

import com.arkivanov.essenty.parcelable.Parcelable

sealed class Screen : Parcelable {
    object Home : Screen()
    object FeedList : Screen()
}
