package com.prof18.feedflow.desktop.about

import com.mikepenz.aboutlibraries.Libs
import org.junit.Test
import kotlin.test.assertTrue

class DesktopLicenseResourcesTest {
    @Test
    fun `bundled license definitions retain their full text`() {
        val json = requireNotNull(javaClass.classLoader.getResourceAsStream("aboutlibraries.json"))
            .bufferedReader().use { it.readText() }
        val report = Libs.Builder().withJson(json).build()

        assertTrue(report.licenses.isNotEmpty(), "The bundled report must include license definitions")
        report.licenses.forEach { license ->
            assertTrue(
                !license.licenseContent.isNullOrBlank(),
                "Missing bundled text for ${license.spdxId ?: license.hash}",
            )
        }
    }
}
