import FeedFlowKit
import Foundation

enum SidebarSelection: Hashable {
    case timeline
    case read
    case bookmarks
    case category(id: String)
    case feedSource(id: String)
}

extension DrawerItem {
    var stableId: String {
        if self is DrawerItem.Timeline {
            return "timeline"
        } else if self is DrawerItem.Read {
            return "read"
        } else if self is DrawerItem.Bookmarks {
            return "bookmarks"
        } else if let category = self as? DrawerItem.DrawerCategory {
            return "category-\(category.category.id)"
        } else if let feedSource = self as? DrawerItem.DrawerFeedSource {
            return "feedsource-\(feedSource.feedSource.id)"
        } else {
            return "unknown-\(ObjectIdentifier(self))"
        }
    }
}
