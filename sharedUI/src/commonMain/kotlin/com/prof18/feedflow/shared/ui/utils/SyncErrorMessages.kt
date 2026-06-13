package com.prof18.feedflow.shared.ui.utils

import com.prof18.feedflow.core.model.ErrorCode
import com.prof18.feedflow.core.model.FeedSyncError
import com.prof18.feedflow.i18n.FeedFlowStrings

fun FeedFlowStrings.syncErrorMessage(errorCode: ErrorCode): String =
    when (errorCode) {
        FeedSyncError.GReaderBadToken -> greaderBadTokenSyncErrorMessage
        else -> syncErrorMessage(errorCode.code)
    }
