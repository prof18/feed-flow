package com.prof18.feedflow.feedsync.database.data

enum class SyncTable(val tableName: String) {
    SYNCED_FEED_SOURCE("synced_feed_source"),
    SYNCED_FEED_SOURCE_CATEGORY("synced_feed_source_category"),
    SYNCED_FEED_ITEM("synced_feed_item"),
}
