package com.prof18.feedflow.desktop.ui.components

import com.prof18.feedflow.core.model.DrawerItem
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.NavDrawerState
import com.prof18.feedflow.desktop.home.projectDesktopFeedTree

internal fun buildSidebarRows(
    navDrawerState: NavDrawerState,
    isUnreadCountHidden: Boolean,
    timelineTitle: String,
    readTitle: String,
    bookmarksTitle: String,
    uncategorizedTitle: String,
): List<SidebarRow> = buildList {
    fun label(title: String, count: Long): String =
        if (isUnreadCountHidden) title else "$title ($count)"

    val timelineCount = navDrawerState.timeline
        .filterIsInstance<DrawerItem.Timeline>()
        .sumOf { it.unreadCount }
    add(
        SidebarRow(
            SidebarEntry(SidebarKey.Timeline, label = label(timelineTitle, timelineCount)),
            FeedFilter.Timeline,
            unreadCount = timelineCount,
        ),
    )
    add(
        SidebarRow(
            SidebarEntry(SidebarKey.Read, label = readTitle),
            FeedFilter.Read,
        ),
    )
    add(
        SidebarRow(
            SidebarEntry(SidebarKey.Bookmarks, label = bookmarksTitle),
            FeedFilter.Bookmarks,
        ),
    )
    val tree = projectDesktopFeedTree(navDrawerState)
    tree.pinned.forEach { item ->
        add(
            SidebarRow(
                SidebarEntry.feed(
                    item.feedSource.id,
                    label(item.feedSource.title, item.unreadCount),
                    isPinned = true,
                ),
                FeedFilter.Source(item.feedSource),
                unreadCount = item.unreadCount,
            ),
        )
    }
    sidebarSections(navDrawerState, uncategorizedTitle)
        .sortedBy { it.position }
        .forEach { section ->
            add(
                SidebarRow(
                    SidebarEntry(
                        section.key,
                        childCount = section.memberCount,
                        label = label(section.title, section.unreadCount),
                    ),
                    section.filter,
                    section = section.id,
                    unreadCount = section.unreadCount,
                ),
            )
            section.children
                .forEach { child ->
                    add(
                        SidebarRow(
                            SidebarEntry.feed(
                                child.feedSource.id,
                                label(child.feedSource.title, child.unreadCount),
                            ),
                            FeedFilter.Source(child.feedSource),
                            parentSection = section.id,
                            unreadCount = child.unreadCount,
                        ),
                    )
                }
        }
}

private fun sidebarSections(
    drawer: NavDrawerState,
    uncategorizedTitle: String,
): List<SidebarSection> = projectDesktopFeedTree(drawer).sections.map { section ->
    val category = section.categoryWrapper.feedSourceCategory
    SidebarSection(
        section.id,
        category?.let { SidebarKey.Category(it.id) } ?: SidebarKey.Uncategorized,
        category?.let { FeedFilter.Category(it) } ?: FeedFilter.Uncategorized,
        category?.title ?: uncategorizedTitle,
        section.unreadCount,
        section.position,
        section.visibleFeeds,
        section.members.size,
    )
}

private data class SidebarSection(
    val id: String,
    val key: SidebarKey,
    val filter: FeedFilter,
    val title: String,
    val unreadCount: Long,
    val position: Int,
    val children: List<DrawerItem.DrawerFeedSource>,
    val memberCount: Int,
)
