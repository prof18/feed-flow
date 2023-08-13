package com.prof18.feedflow.utils

// The NSXMLParser loads the entire feed in memory, so better be more conservative
actual fun getNumberOfConcurrentParsingRequests(): Int = 10
actual fun getNumberOfConcurrentFeedSavers(): Int = 10

actual fun getLimitedNumberOfConcurrentParsingRequests(): Int = 4

actual fun getLimitedNumberOfConcurrentFeedSavers(): Int = 4
