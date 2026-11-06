package com.prof18.feedflow.android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InAppBrowserPackageTest {

    @Test
    fun `no default browser pins the custom tabs provider instead of showing the chooser`() {
        var candidates: List<String>? = null
        val result = inAppBrowserPackage(
            resolvedPackage = SYSTEM_RESOLVER,
            handlerPackages = listOf(CHROME, FIREFOX),
            customTabsProvider = {
                candidates = it
                CHROME
            },
        )

        assertEquals(CHROME, result)
        assertEquals(listOf(CHROME, FIREFOX), candidates)
    }

    @Test
    fun `unresolved link pins the custom tabs provider`() {
        val result = inAppBrowserPackage(
            resolvedPackage = null,
            handlerPackages = listOf(CHROME, FIREFOX),
            customTabsProvider = { FIREFOX },
        )

        assertEquals(FIREFOX, result)
    }

    @Test
    fun `default browser leaves routing to the system`() {
        var providerQueried = false
        val result = inAppBrowserPackage(
            resolvedPackage = FIREFOX,
            handlerPackages = listOf(CHROME, FIREFOX),
            customTabsProvider = {
                providerQueried = true
                CHROME
            },
        )

        assertNull(result)
        assertEquals(false, providerQueried)
    }

    @Test
    fun `verified app link handler keeps opening its app`() {
        val result = inAppBrowserPackage(
            resolvedPackage = NEWS_APP,
            handlerPackages = listOf(NEWS_APP, CHROME, FIREFOX),
            customTabsProvider = { CHROME },
        )

        assertNull(result)
    }

    @Test
    fun `no custom tabs provider leaves routing to the system`() {
        val result = inAppBrowserPackage(
            resolvedPackage = SYSTEM_RESOLVER,
            handlerPackages = listOf(CHROME, FIREFOX),
            customTabsProvider = { null },
        )

        assertNull(result)
    }

    private companion object {
        const val SYSTEM_RESOLVER = "android"
        const val CHROME = "com.android.chrome"
        const val FIREFOX = "org.mozilla.firefox"
        const val NEWS_APP = "com.google.android.apps.magazines"
    }
}
