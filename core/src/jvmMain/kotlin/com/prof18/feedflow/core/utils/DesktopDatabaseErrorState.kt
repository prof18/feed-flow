package com.prof18.feedflow.core.utils

import kotlinx.coroutines.flow.MutableStateFlow

object DesktopDatabaseErrorState {
    /** Name of the recovery copy kept after a corrupt database was replaced, until the notice is shown. */
    val recoveredFileName = MutableStateFlow<String?>(null)

    fun setRecovered(name: String) {
        recoveredFileName.value = name
    }

    fun clear() {
        recoveredFileName.value = null
    }
}
