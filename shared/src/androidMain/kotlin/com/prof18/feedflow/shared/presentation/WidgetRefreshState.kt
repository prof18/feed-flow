package com.prof18.feedflow.shared.presentation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class WidgetRefreshState {
    private val refreshing = MutableStateFlow(false)
    val isRefreshing = refreshing.asStateFlow()

    fun setRefreshing(value: Boolean) {
        refreshing.value = value
    }
}
