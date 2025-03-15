package com.prof18.feedflow.shared.data

import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ExperimentalSettingsImplementation
import com.russhwolf.settings.KeychainSettings
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleAfterFirstUnlock
import platform.Security.kSecAttrService
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.ref.createCleaner

@OptIn(ExperimentalNativeApi::class, ExperimentalForeignApi::class, ExperimentalSettingsApi::class)
object KeychainSettingsWrapper {

    private val cfService = CFBridgingRetain("FeedFlow2")

    @OptIn(ExperimentalSettingsImplementation::class)
    val settings = KeychainSettings(
        kSecAttrService to cfService,
        kSecAttrAccessible to kSecAttrAccessibleAfterFirstUnlock,
    )

    @Suppress("UnusedPrivateProperty")
    private val cleaner = createCleaner(cfService) { CFBridgingRelease(it) }
}
