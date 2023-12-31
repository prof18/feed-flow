package com.prof18.feedflow.utils

// The NSXMLParser loads the entire feed in memory, so better be more conservative
internal actual fun getNumberOfConcurrentParsingRequests(): Int = 5
