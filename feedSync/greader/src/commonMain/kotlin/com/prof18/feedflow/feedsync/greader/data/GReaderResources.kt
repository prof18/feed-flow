package com.prof18.feedflow.feedsync.greader.data

import io.ktor.resources.Resource

@Suppress("ConstructorParameterNaming")
@Resource("accounts")
class AccountsRes {
    @Resource("ClientLogin")
    class ClientLogin(
        val parent: AccountsRes = AccountsRes(),
        val Email: String,
        val Passwd: String,
    )
}

@Suppress("ConstructorParameterNaming")
@Resource("reader")
class ReaderResource {

    @Resource("api")
    class Api(
        val parent: ReaderResource = ReaderResource(),
    ) {
        @Resource("0")
        class Zero(
            val parent: Api = Api(),
        ) {
            @Resource("subscription")
            class Subscription(
                val parent: Zero = Zero(),
            ) {
                @Resource("list")
                class List(
                    val parent: Subscription = Subscription(),
                    val output: String = "json",
                )

                @Resource("edit")
                class Edit(
                    val parent: Subscription = Subscription(),
                )

                @Resource("quickadd")
                class QuickAdd(
                    val parent: Subscription = Subscription(),
                    val output: String = "json",
                )
            }

            @Resource("stream")
            class StreamRes(
                val parent: Zero = Zero(),
            ) {

                @Resource("contents")
                class Contents(
                    val parent: StreamRes = StreamRes(),
                ) {

                    @Resource("user/-/state/com.google/reading-list")
                    class ReadingList(
                        val parent: Contents = Contents(),
                        // Exclude target
                        val xt: List<String>? = null,
                        // Maximum number of items to return.
                        val n: Int,
                        // Epoch timestamp. Items older than this timestamp are filtered out.
                        val ot: Long? = null,
                        // continuation
                        val c: String? = null,
                        val output: String = "json",
                    )

                    @Resource("user/-/state/com.google/starred")
                    class Starred(
                        val parent: Contents = Contents(),
                        // Maximum number of items to return.
                        val n: Int,
                        // Epoch timestamp. Items older than this timestamp are filtered out.
                        val ot: Long? = null,
                        // continuation
                        val c: String? = null,
                        val output: String = "json",
                    )
                }

                @Resource("items")
                class Items(
                    val parent: StreamRes = StreamRes(),
                ) {
                    @Resource("ids")
                    class IDs(
                        val parent: Items = Items(),
                        // stream ID
                        val s: String,
                        /** Epoch timestamp. Items older than this timestamp are filtered out. */
                        val ot: Long? = null,
                        // continuation
                        val c: String? = null,
                        // count
                        val n: Int = 15_000,
                        // A stream ID to exclude.
                        val xt: String? = null,
                        val output: String = "json",
                    )

                    @Resource("contents")
                    class Contents(
                        val parent: Items = Items(),
                        val output: String = "json",
                    )
                }
            }

            @Resource("token")
            class Token(val parent: Zero = Zero())

            @Resource("edit-tag")
            class EditTag(
                val parent: Zero = Zero(),
                val output: String = "json",
            )

            @Resource("rename-tag")
            class RenameTag(
                val parent: Zero = Zero(),
            )

            @Resource("disable-tag")
            class DisableTag(
                val parent: Zero = Zero(),
            )
        }
    }
}
