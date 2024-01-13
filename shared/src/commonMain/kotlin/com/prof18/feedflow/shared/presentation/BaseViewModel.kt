package com.prof18.feedflow.shared.presentation

import kotlinx.coroutines.CoroutineScope

expect abstract class BaseViewModel() {

    internal val scope: CoroutineScope

    fun clear()
}
