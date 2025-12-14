package com.prof18.feedflow.shared.utils

import com.prof18.feedflow.core.utils.AppConfig
import io.ktor.http.encodeURLParameter

class UserFeedbackReporter(
    private val appConfig: AppConfig,
) {
    fun getEmailUrl(
        subject: String,
        content: String,
    ): String {
        val creationLine = "\n\n\n----------------------------\n"
        val appVersion = "App Version: ${appConfig.appVersion}\n"
        val platformName = "Platform: ${appConfig.platformName}\n"
        val platformVersion = "Platform Version: ${appConfig.platformVersion}\n"

        val body = "$content$creationLine$appVersion$platformName$platformVersion"

        val escapedSubject = subject.encodeURLParameter()
        // Ensure newlines are CRLF for correct display in some email clients (like iOS Mail)
        val escapedBody = body.replace("\n", "\r\n").encodeURLParameter()

        return """
            mailto:$SUPPORT_EMAIL?subject=$escapedSubject&body=$escapedBody
        """.trimIndent()
    }

    private companion object {
        const val SUPPORT_EMAIL = "mgp.dev.studio+feedflow@gmail.com"
    }
}
