import FeedFlowKit
import Foundation
import WidgetKit

struct Provider: AppIntentTimelineProvider {
    func placeholder(in _: Context) -> WidgetEntry {
        let strings = WidgetSupport.strings
        return WidgetEntry(
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
        let contentSelection = WidgetContentSelection(selectionId: selection?.id)
        let items = getFeedItems(
            appEnvironment: WidgetSupport.appEnvironment,
            filterType: contentSelection.filterType,
            filterId: contentSelection.filterId
        )
        let title = switch contentSelection.filterType {
        case "bookmarks":
            strings.widgetContentBookmarks
        case "category", "source":
            selection?.title ?? strings.widgetTitle
        default:
            strings.widgetTitle
        }
        let emptyTitle = contentSelection.filterType == "bookmarks"
            ? strings.widgetBookmarksEmptyMessage
            : strings.widgetEmptyScreenTitle
        return WidgetEntry(
            date: date,
            feedItems: items,
            widgetTitle: title,
            widgetEmptyScreenTitle: emptyTitle,
            widgetEmptyScreenContent: strings.widgetEmptyScreenContent
        )
    }
}
