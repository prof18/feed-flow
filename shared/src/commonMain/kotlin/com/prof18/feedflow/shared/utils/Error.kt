package com.prof18.feedflow.shared.utils

import com.prof18.rssparser.exception.HttpException
import kotlinx.coroutines.CancellationException

fun Throwable.skipLogging(): Boolean =
    this is HttpException || this.ignoreError() || this is CancellationException

expect fun Throwable.ignoreError(): Boolean
