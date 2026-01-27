package com.prof18.feedflow.shared.utils

import com.prof18.feedflow.shared.test.KoinTestBase
import io.ktor.http.encodeURLParameter
import org.koin.test.inject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UserFeedbackReporterTest : KoinTestBase() {

    private val reporter: UserFeedbackReporter by inject()

    @Test
    fun `getEmailUrl formats mailto link with encoded subject and body`() {
        val subject = "Need Help"
        val content = "Hello\nWorld"

        val result = reporter.getEmailUrl(
            subject = subject,
            content = content,
        )

        val creationLine = "\n\n\n----------------------------\n"
        val body = "$content$creationLine" +
            "App Version: 1.0.0\n" +
            "Platform: Test\n" +
            "Platform Version: 1.0.0\n" +
            "Sync Account: local\n"
        val expectedSubject = subject.encodeURLParameter()
        val expectedBody = body.replace("\n", "\r\n").encodeURLParameter()

        assertEquals(
            "mailto:mgp.dev.studio+feedflow@gmail.com?subject=$expectedSubject&body=$expectedBody",
            result,
        )
        assertTrue(result.contains("%0D%0A"))
    }
}
