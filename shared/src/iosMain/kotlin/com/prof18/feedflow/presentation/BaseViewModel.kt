package com.prof18.feedflow.presentation

import com.rickclephas.kmp.nativecoroutines.NativeCoroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

actual abstract class BaseViewModel {

    @NativeCoroutineScope
    actual val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    actual fun clear() {
        scope.cancel()
    }
}
