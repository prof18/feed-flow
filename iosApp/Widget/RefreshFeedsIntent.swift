import AppIntents
import FeedFlowKit
import WidgetKit

struct RefreshFeedsIntent: AppIntent {
    static let title: LocalizedStringResource = "Refresh feeds"

    func perform() async throws -> some IntentResult {
        do {
            try await WidgetRefreshCoordinator.shared.refresh {
                WidgetKoin.ensureStarted()
                try await Deps.shared.getSerialFeedFetcherRepository().fetchFeeds(forceRefresh: true)
            }
        } catch {
            WidgetCenter.shared.reloadTimelines(ofKind: FeedFlowWidget.kind)
            throw error
        }
        return .result()
    }
}
