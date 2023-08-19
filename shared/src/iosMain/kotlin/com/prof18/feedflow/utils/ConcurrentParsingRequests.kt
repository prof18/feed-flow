package com.prof18.feedflow.utils

// The NSXMLParser loads the entire feed in memory, so better be more conservative
actual fun getNumberOfConcurrentParsingRequests(): Int = 4
