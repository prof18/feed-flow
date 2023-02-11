package com.prof18.feedflow

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform

expect fun getDateMillisFromString(dateString: String): Long?