package com.prof18.feedflow.shared.data

import com.russhwolf.settings.KeychainSettings
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSBundle
import platform.Security.kSecAttrAccessGroup
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleAfterFirstUnlock
import platform.Security.kSecAttrService
import kotlin.native.ref.createCleaner

object KeychainSettingsWrapper {

    private val cfService = CFBridgingRetain("FeedFlow2")
    private val cfAccessGroup = NSBundle.mainBundle.objectForInfoDictionaryKey("FeedFlowKeychainAccessGroup")
        ?.let { CFBridgingRetain(it) }

    val settings = if (cfAccessGroup == null) {
        KeychainSettings(
            kSecAttrService to cfService,
            kSecAttrAccessible to kSecAttrAccessibleAfterFirstUnlock,
        )
    } else {
        // Keep extension queries in the host's group instead of matching old extension-private settings.
        KeychainSettings(
            kSecAttrService to cfService,
            kSecAttrAccessible to kSecAttrAccessibleAfterFirstUnlock,
            kSecAttrAccessGroup to cfAccessGroup,
        )
    }

    @Suppress("UnusedPrivateProperty")
    private val cleaner = createCleaner(cfService) { CFBridgingRelease(it) }

    @Suppress("UnusedPrivateProperty")
    private val accessGroupCleaner = cfAccessGroup?.let { accessGroup ->
        createCleaner(accessGroup) { CFBridgingRelease(it) }
    }
}
