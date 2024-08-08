package com.prof18.feedflow.shared.presentation

import androidx.lifecycle.ViewModel
import com.prof18.feedflow.core.model.SyncAccounts
import com.prof18.feedflow.shared.domain.feedsync.AccountsRepository

class AccountsViewModel internal constructor(
    private val accountsRepository: AccountsRepository,
) : ViewModel() {

    val accountsState = accountsRepository.currentAccountState

    fun getSupportedAccounts(): List<SyncAccounts> = accountsRepository.getValidAccounts()

    fun setICloudAccount() {
        accountsRepository.setICloudAccount()
    }
}
