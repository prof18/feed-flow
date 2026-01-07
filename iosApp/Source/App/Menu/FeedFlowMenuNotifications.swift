import Foundation

extension Notification.Name {
    static let feedFlowRefreshFeeds = Notification.Name("feedflow.menu.refresh-feeds")
    static let feedFlowForceRefreshFeeds = Notification.Name("feedflow.menu.force-refresh-feeds")
    static let feedFlowMarkAllRead = Notification.Name("feedflow.menu.mark-all-read")
    static let feedFlowClearOldArticles = Notification.Name("feedflow.menu.clear-old-articles")
    static let feedFlowAddFeed = Notification.Name("feedflow.menu.add-feed")
    static let feedFlowEditCurrentFeed = Notification.Name("feedflow.menu.edit-current-feed")
    static let feedFlowImportExport = Notification.Name("feedflow.menu.import-export")
    static let feedFlowOpenSettings = Notification.Name("feedflow.menu.open-settings")
}
