package com.prof18.feedflow.presentation

import kotlinx.coroutines.CoroutineScope

expect abstract class BaseViewModel() {

    internal val scope: CoroutineScope

    fun clear()
}
