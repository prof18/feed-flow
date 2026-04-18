package com.prof18.feedflow.shared.test.generators

import com.prof18.feedflow.core.model.SyncAccounts

object AccountGenerator {
    fun syncAccount(account: SyncAccounts = SyncAccounts.LOCAL): SyncAccounts = account
}
