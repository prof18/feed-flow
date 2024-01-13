package com.prof18.feedflow.shared

import com.prof18.feedflow.shared.utils.DispatcherProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher

object TestDispatcherProvider : DispatcherProvider {

    @OptIn(ExperimentalCoroutinesApi::class)
    val testDispatcher = UnconfinedTestDispatcher()

    override val io: CoroutineDispatcher = testDispatcher

    override val main: CoroutineDispatcher = testDispatcher

    override val default: CoroutineDispatcher = testDispatcher
}
