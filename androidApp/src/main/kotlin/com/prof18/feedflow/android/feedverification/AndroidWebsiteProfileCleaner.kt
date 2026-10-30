package com.prof18.feedflow.android.feedverification

import com.prof18.feedflow.shared.domain.recovery.FeedRecoveryRepository
import com.prof18.feedflow.shared.domain.recovery.WebsiteProfileCleaner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Deletes the WebView data of retired recovery profiles. Users who never verified a website have
 * no profiles, so this never touches WebView for them.
 */
internal class AndroidWebsiteProfileCleaner(
    private val repository: FeedRecoveryRepository,
    private val webViewProfiles: WebViewProfiles,
) : WebsiteProfileCleaner {

    override suspend fun clean() {
        if (!repository.hasProfiles()) return
        val epoch = repository.accountEpochState.value
        repository.housekeeping(epoch)
        withContext(Dispatchers.Main.immediate) {
            if (!webViewProfiles.isSupported()) return@withContext
            val names = webViewProfiles.names()
            repository.profiles()
                .filter { !it.retired && it.accountEpoch == epoch && it.id !in names }
                .forEach { repository.retireMissingProfile(it.id) }
            repository.profiles().filter { it.retired }.forEach { profile ->
                if (webViewProfiles.clearAndDelete(profile.id)) {
                    repository.nativeDeletionCompleted(profile.id)
                }
            }
        }
    }
}
