package com.prof18.feedflow.core.utils

import kotlinx.coroutines.flow.MutableStateFlow

object DesktopDatabaseErrorState {
    val errorState = MutableStateFlow(false)

    fun setError(error: Boolean) {
        errorState.value = error
    }
}
