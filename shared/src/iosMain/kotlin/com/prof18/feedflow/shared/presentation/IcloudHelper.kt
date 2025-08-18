package com.prof18.feedflow.shared.presentation

import kotlinx.coroutines.delay
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import kotlin.time.Clock

internal suspend fun getICloudBaseFolderURL(
    timeoutSeconds: Int = 30,
    initialPollIntervalMs: Long = 500,
): NSURL? {
    val startTime = Clock.System.now()
    var currentPollInterval = initialPollIntervalMs

    while ((Clock.System.now() - startTime).inWholeSeconds < timeoutSeconds) {
        val url = NSFileManager.defaultManager
            .URLForUbiquityContainerIdentifier("iCloud.com.prof18.feedflow")
            ?.URLByAppendingPathComponent("Documents")

        if (url != null) {
            return url
        }

        delay(currentPollInterval)
        @Suppress("MagicNumber")
        currentPollInterval = (currentPollInterval * 1.5).toLong().coerceAtMost(maximumValue = 5000L)
    }

    return null
}
