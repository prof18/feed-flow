package com.prof18.feedflow.core.model

data class ArticleReportUiState(
    val canReport: Boolean = false,
    val sending: Boolean = false,
    val showDisclosure: Boolean = false,
    val message: ArticleReportMessage? = null,
)

enum class ArticleReportMessage {
    SENDING,
    SENT,
    FAILED,
}
