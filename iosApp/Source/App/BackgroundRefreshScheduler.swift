import BackgroundTasks
import Foundation

// iOS gets two background tasks. The app refresh runs several times a day but has about 30
// seconds, so it only fetches the local feeds that are due, within a budget, and then posts
// notifications. The processing task runs rarely, usually overnight on the charger, with minutes
// to spare: it does the full fetch, account and cloud backup sync included.
enum BackgroundRefreshScheduler {
    static let refreshTaskIdentifier = "com.prof18.feedflow.articlerefresh"
    static let fullSyncTaskIdentifier = "com.prof18.feedflow.articlesync"
    static let taskIdentifiers = [refreshTaskIdentifier, fullSyncTaskIdentifier]

    static let minimumRefreshInterval: TimeInterval = 3_600
    static let minimumFullSyncInterval: TimeInterval = 2 * 3_600
    // Leaves about a third of the app refresh time for the widget reload and the notifications.
    static let refreshTimeBudgetMillis: Int64 = 20_000

    static func makeRefreshRequest(now: Date = .now) -> BGAppRefreshTaskRequest {
        let request = BGAppRefreshTaskRequest(identifier: refreshTaskIdentifier)
        request.earliestBeginDate = now.addingTimeInterval(minimumRefreshInterval)
        return request
    }

    static func makeFullSyncRequest(now: Date = .now) -> BGProcessingTaskRequest {
        let request = BGProcessingTaskRequest(identifier: fullSyncTaskIdentifier)
        request.earliestBeginDate = now.addingTimeInterval(minimumFullSyncInterval)
        request.requiresNetworkConnectivity = true
        return request
    }

    static func schedule(scheduler: BGTaskScheduler = .shared) {
        for request in [makeRefreshRequest(), makeFullSyncRequest()] as [BGTaskRequest] {
            do {
                try scheduler.submit(request)
            } catch {
                print("Could not schedule background task \(request.identifier): \(error)")
            }
        }
    }
}
