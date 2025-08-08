package com.prof18.feedflow.shared.utils

import com.prof18.rssparser.exception.HttpException

fun Throwable.skipLogging(): Boolean =
    this is HttpException || this.ignoreError()

expect fun Throwable.ignoreError(): Boolean
