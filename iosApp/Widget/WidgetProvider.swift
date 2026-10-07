import FeedFlowKit
import Foundation
import WidgetKit

struct Provider: AppIntentTimelineProvider {
    func placeholder(in _: Context) -> WidgetEntry {
        let strings = WidgetSupport.strings
        return WidgetEntry(
            showRefreshButton: false,
            refreshLabel: strings.widgetRefresh,
            date: Date(),
            feedItems: [],
            widgetTitle: strings.widgetTitle,
            widgetEmptyScreenTitle: strings.widgetEmptyScreenTitle,
            widgetEmptyScreenContent: strings.widgetEmptyScreenContent
        )
    }

    func snapshot(
        for configuration: FeedFlowWidgetConfigurationIntent,
        in _: Context
    ) async -> WidgetEntry {
        makeEntry(for: configuration)
    }

    func timeline(
        for configuration: FeedFlowWidgetConfigurationIntent,
        in _: Context
    ) async -> Timeline<WidgetEntry> {
        let currentDate = Date()
        let refreshDate = Calendar.current.date(
            byAdding: .hour,
            value: 1,
            to: currentDate
        ) ?? currentDate
        let entry = makeEntry(for: configuration, date: currentDate)
        return Timeline(entries: [entry], policy: .after(refreshDate))
    }

    private func makeEntry(
        for configuration: FeedFlowWidgetConfigurationIntent,
        date: Date = Date()
    ) -> WidgetEntry {
        let strings = WidgetSupport.strings
        let selection = configuration.content
        let (filterType, filterId) = Self.parse(selectionId: selection?.id)
        let items = getFeedItems(
            appEnvironment: WidgetSupport.appEnvironment,
            filterType: filterType,
            filterId: filterId
        )
        let title = switch filterType {
        case "bookmarks":
            strings.widgetContentBookmarks
        case "category", "source":
            selection?.title ?? strings.widgetTitle
        default:
            strings.widgetTitle
        }
        let emptyTitle = filterType == "bookmarks"
            ? strings.widgetBookmarksEmptyMessage
            : strings.widgetEmptyScreenTitle
        return WidgetEntry(
            showRefreshButton: configuration.showRefreshButton,
            refreshLabel: strings.widgetRefresh,
            date: date,
            feedItems: items,
            widgetTitle: title,
            widgetEmptyScreenTitle: emptyTitle,
            widgetEmptyScreenContent: strings.widgetEmptyScreenContent
        )
    }

    private static func parse(selectionId: String?) -> (String, String?) {
        guard let selectionId else {
            return ("timeline", nil)
        }
        if selectionId == "timeline" || selectionId == "bookmarks" {
            return (selectionId, nil)
        }

        let categoryPrefix = "category:"
        if selectionId.hasPrefix(categoryPrefix) {
            return ("category", String(selectionId.dropFirst(categoryPrefix.count)))
        }

        let sourcePrefix = "source:"
        if selectionId.hasPrefix(sourcePrefix) {
            return ("source", String(selectionId.dropFirst(sourcePrefix.count)))
        }
        return ("timeline", nil)
    }
}
