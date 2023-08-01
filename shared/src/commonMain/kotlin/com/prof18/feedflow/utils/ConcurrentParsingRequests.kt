package com.prof18.feedflow.utils

expect fun getNumberOfConcurrentParsingRequests(): Int
expect fun getNumberOfConcurrentFeedSavers(): Int
expect fun getLimitedNumberOfConcurrentParsingRequests(): Int
expect fun getLimitedNumberOfConcurrentFeedSavers(): Int
