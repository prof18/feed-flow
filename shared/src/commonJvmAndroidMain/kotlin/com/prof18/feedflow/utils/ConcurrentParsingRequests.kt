package com.prof18.feedflow.utils

actual fun getNumberOfConcurrentParsingRequests(): Int = 20

actual fun getNumberOfConcurrentFeedSavers(): Int = 20

actual fun getLimitedNumberOfConcurrentParsingRequests(): Int = 20

actual fun getLimitedNumberOfConcurrentFeedSavers(): Int = 20
