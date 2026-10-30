package com.prof18.feedflow.shared.presentation.model

import com.prof18.feedflow.core.model.FeedSyncError
import com.prof18.feedflow.core.model.NoCode
import com.prof18.feedflow.core.model.SyncUploadError
import com.prof18.feedflow.i18n.DeFeedFlowStrings
import com.prof18.feedflow.i18n.EnFeedFlowStrings
import com.prof18.feedflow.shared.test.KoinTestBase
import kotlin.test.Test
import kotlin.test.assertEquals

class SyncErrorMessagesTest : KoinTestBase() {
    @Test
    fun `bad reader token explains how to reconnect`() {
        assertEquals(
            EnFeedFlowStrings.greaderBadTokenSyncErrorMessage,
            EnFeedFlowStrings.syncErrorMessage(FeedSyncError.GReaderBadToken),
        )
    }

    @Test
    fun `specialized error uses the requested language`() {
        assertEquals(
            DeFeedFlowStrings.greaderBadTokenSyncErrorMessage,
            DeFeedFlowStrings.syncErrorMessage(FeedSyncError.GReaderBadToken),
        )
    }

    @Test
    fun `other errors retain their support code in localized text`() {
        assertEquals(
            DeFeedFlowStrings.syncErrorMessage("SU2"),
            DeFeedFlowStrings.syncErrorMessage(SyncUploadError.DropboxUploadFailed),
        )
    }

    @Test
    fun `missing error code still produces the generic message`() {
        assertEquals(EnFeedFlowStrings.syncErrorMessage(""), EnFeedFlowStrings.syncErrorMessage(NoCode))
    }
}
