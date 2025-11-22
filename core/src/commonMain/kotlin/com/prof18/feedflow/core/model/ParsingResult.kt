package com.prof18.feedflow.core.model

sealed class ParsingResult {

    data class Success(
        val htmlContent: String?,
        val title: String?,
        val siteName: String?,
    ) : ParsingResult()

    data object Error : ParsingResult()
}
