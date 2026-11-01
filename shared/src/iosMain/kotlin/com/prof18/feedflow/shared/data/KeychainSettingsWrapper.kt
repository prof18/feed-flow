package com.prof18.feedflow.shared.data

import com.russhwolf.settings.KeychainSettings
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleAfterFirstUnlock
import platform.Security.kSecAttrService
import kotlin.native.ref.createCleaner

object KeychainSettingsWrapper {

    private val cfService = CFBridgingRetain("FeedFlow2")

    val settings = KeychainSettings(
        kSecAttrService to cfService,
        kSecAttrAccessible to kSecAttrAccessibleAfterFirstUnlock,
    )

    @Suppress("UnusedPrivateProperty")
    private val cleaner = createCleaner(cfService) { CFBridgingRelease(it) }
}
