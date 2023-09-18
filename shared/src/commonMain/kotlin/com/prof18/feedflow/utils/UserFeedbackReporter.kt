package com.prof18.feedflow.utils

object UserFeedbackReporter {
    private const val SUPPORT_EMAIL = "mgp.dev.studio+feedflow@gmail.com"

    fun getEmailUrl(
        subject: String,
        content: String,
    ): String {
        val escapedContent = content
            .replace(" ", "%20")
        val escapedSubject = subject.replace(" ", "%20")

        return """
            mailto:$SUPPORT_EMAIL?subject=$escapedSubject&body=$escapedContent
        """.trimIndent()
    }
}
