package com.prof18.feedflow.shared.e2e

enum class E2eSeedProfile(val queryValue: String) {
    EMPTY("empty"),
    CONTENT_RICH("content-rich"),
    CARD_LAYOUT("card-layout"),
    COMPACT_LIST("compact-list"),
    READER_MODE("reader-mode"),
    EXTERNAL_BROWSER("external-browser"),
    READ_BEHAVIOR("read-behavior"),
    OLDEST_FIRST("oldest-first"),
    NOTIFICATIONS("notifications"),
    ANDROID_WIDGET("android-widget"),
    SYNC_LINKED_MOCK("sync-linked-mock"),
    ;

    companion object {
        fun fromQueryValue(value: String?): E2eSeedProfile? =
            entries.firstOrNull { profile ->
                profile.queryValue == value
            }
    }
}
