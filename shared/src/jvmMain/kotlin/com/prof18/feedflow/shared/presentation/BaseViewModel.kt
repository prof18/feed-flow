package com.prof18.feedflow.shared.presentation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

actual abstract class BaseViewModel {

    actual val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    actual fun clear() {
        scope.cancel()
    }
}
