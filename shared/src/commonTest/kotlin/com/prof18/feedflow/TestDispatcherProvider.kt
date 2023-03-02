package com.prof18.feedflow

import com.prof18.feedflow.utils.DispatcherProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher

object TestDispatcherProvider : DispatcherProvider {

    val testDispatcher = UnconfinedTestDispatcher()

    override val io: CoroutineDispatcher = testDispatcher

    override val main: CoroutineDispatcher = testDispatcher

    override val default: CoroutineDispatcher = testDispatcher

}