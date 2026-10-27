import Foundation

/// Splits widget content options into the sections shown by the configuration picker.
struct WidgetContentGroups<Item: WidgetContentListing> {
    let builtIns: [Item]
    let categories: [Item]
    let sources: [Item]

    init(_ items: [Item]) {
        builtIns = items.filter { $0.id == "timeline" || $0.id == "bookmarks" }
        categories = Self.sortedByTitle(items.filter { $0.isCategory == true })
        sources = Self.sortedByTitle(items.filter { $0.id.hasPrefix("source:") })
    }

    private static func sortedByTitle(_ items: [Item]) -> [Item] {
        items.sorted {
            $0.title.compare(
                $1.title,
                options: [.caseInsensitive, .diacriticInsensitive],
                locale: .current
            ) == .orderedAscending
        }
    }
}
