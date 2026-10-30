import FeedFlowKit

final class WidgetNotifier: Notifier {
    func showNewArticlesNotification(feedSourcesToNotify _: [FeedSourceToNotify]) -> Bool {
        false
    }
}
