import FeedFlowKit

enum SearchAccessibilityIdentifiers {
    static func filter(_ filter: SearchFilter) -> String {
        switch filter {
        case .currentFeed:
            return "search_filter_current_feed"
        case .all:
            return "search_filter_all"
        case .read:
            return "search_filter_read"
        case .bookmarks:
            return "search_filter_bookmarks"
        }
    }
}
