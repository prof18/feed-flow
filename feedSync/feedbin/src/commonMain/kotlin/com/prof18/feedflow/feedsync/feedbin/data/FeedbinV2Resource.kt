package com.prof18.feedflow.feedsync.feedbin.data

import io.ktor.resources.Resource

@Suppress("ConstructorParameterNaming", "unused")
@Resource("v2")
class FeedbinV2Resource {

    @Resource("subscriptions.json")
    class Subscriptions(
        val parent: FeedbinV2Resource = FeedbinV2Resource(),
        val since: String? = null,
    )

    @Resource("subscriptions")
    class SubscriptionsBase(
        val parent: FeedbinV2Resource = FeedbinV2Resource(),
    ) {
        @Resource("{id}")
        class ById(
            val parent: SubscriptionsBase,
            val id: String,
        )
    }

    @Resource("entries.json")
    class Entries(
        val parent: FeedbinV2Resource = FeedbinV2Resource(),
        val page: Int? = null,
        val since: String? = null,
        val ids: String? = null,
        val mode: String? = null,
        val read: Boolean? = null,
        val starred: Boolean? = null,
        val per_page: Int? = null,
    )

    @Resource("entries")
    class EntriesBase(
        val parent: FeedbinV2Resource = FeedbinV2Resource(),
    ) {
        @Resource("{id}")
        class ById(
            val parent: EntriesBase,
            val id: String,
        )
    }

    @Resource("feeds")
    class Feeds(
        val parent: FeedbinV2Resource = FeedbinV2Resource(),
    ) {
        @Resource("{feed_id}")
        class ByFeedId(
            val parent: Feeds = Feeds(),
            val feed_id: Long,
        ) {
            @Resource("entries.json")
            class Entries(
                val parent: ByFeedId,
                val page: Int? = null,
                val since: String? = null,
            )
        }
    }

    @Resource("unread_entries.json")
    class UnreadEntries(
        val parent: FeedbinV2Resource = FeedbinV2Resource(),
    )

    @Resource("starred_entries.json")
    class StarredEntries(
        val parent: FeedbinV2Resource = FeedbinV2Resource(),
    )

    @Resource("taggings.json")
    class Taggings(
        val parent: FeedbinV2Resource = FeedbinV2Resource(),
    )

    @Resource("taggings")
    class TaggingsBase(
        val parent: FeedbinV2Resource = FeedbinV2Resource(),
    ) {
        @Resource("{id}")
        class ById(
            val parent: TaggingsBase,
            val id: String,
        )
    }

    @Resource("tags.json")
    class Tags(
        val parent: FeedbinV2Resource = FeedbinV2Resource(),
    )

    @Resource("icons.json")
    class Icons(
        val parent: FeedbinV2Resource = FeedbinV2Resource(),
    )

    @Resource("authentication.json")
    class Authentication(
        val parent: FeedbinV2Resource = FeedbinV2Resource(),
    )
}
