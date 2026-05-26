package com.prof18.feedflow.shared.e2e

import com.prof18.feedflow.core.model.SyncAccounts

enum class E2eSeedProfile(val queryValue: String) {
    EMPTY("empty"),
    CONTENT_RICH("content-rich"),
    CARD_LAYOUT("card-layout"),
    COMPACT_LIST("compact-list"),
    READER_MODE("reader-mode"),
    EXTERNAL_BROWSER("external-browser"),
    READ_BEHAVIOR("read-behavior"),
    OLDEST_FIRST("oldest-first"),
    SWIPE_ACTIONS("swipe-actions"),
    SWIPE_DISABLED("swipe-disabled"),
    NOTIFICATIONS("notifications"),
    ANDROID_WIDGET("android-widget"),
    SYNC_LINKED_MOCK("sync-linked-mock"),
    SYNC_UPLOAD_REQUIRED("sync-upload-required"),
    ;

    companion object {
        fun fromQueryValue(value: String?): E2eSeedProfile? =
            entries.firstOrNull { profile ->
                profile.queryValue == value
            }
    }
}

enum class E2eSeedAccount(
    val queryValue: String,
    val syncAccount: SyncAccounts,
) {
    DROPBOX("dropbox", SyncAccounts.DROPBOX),
    GOOGLE_DRIVE("google_drive", SyncAccounts.GOOGLE_DRIVE),
    ICLOUD("icloud", SyncAccounts.ICLOUD),
    FRESH_RSS("fresh_rss", SyncAccounts.FRESH_RSS),
    MINIFLUX("miniflux", SyncAccounts.MINIFLUX),
    FEEDBIN("feedbin", SyncAccounts.FEEDBIN),
    BAZQUX("bazqux", SyncAccounts.BAZQUX),
    ;

    companion object {
        fun fromQueryValue(value: String?): E2eSeedAccount? =
            entries.firstOrNull { account ->
                account.queryValue == value
            }
    }
}
