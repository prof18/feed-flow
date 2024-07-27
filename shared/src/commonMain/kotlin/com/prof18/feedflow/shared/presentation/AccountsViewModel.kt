package com.prof18.feedflow.shared.presentation

import com.prof18.feedflow.shared.domain.accounts.AccountsRepository
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState

class AccountsViewModel internal constructor(
    private val accountsRepository: AccountsRepository,
) : BaseViewModel() {

    @NativeCoroutinesState
    val accountsState = accountsRepository.currentAccountState
}
