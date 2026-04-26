import BackgroundTasks
import FeedFlowKit
import FirebaseCore
import FirebaseCrashlytics
import Foundation
import SwiftUI
import SwiftyDropbox
import TelemetryDeck
import UserNotifications
import WidgetKit

@main
struct FeedFlowApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self)
    private var delegate
    @Environment(\.scenePhase)
    private var scenePhase: ScenePhase
    @State private var appState: AppState = .init()
    @State private var browserSelector: BrowserSelector

    private var feedSyncTimer: FeedSyncTimer = .init()

    init() {
        startKoin(notifier: IOSNotifier())
        setupTelemetry()
        _browserSelector = State(initialValue: BrowserSelector())

        if let path = Bundle.main.path(forResource: "Info", ofType: "plist") {
            if let keys = NSDictionary(contentsOfFile: path) {
                let key = keys["DropboxApiKey"] as? String ?? ""
                let dropboxDataSource = Deps.shared.getDropboxDataSource()
                dropboxDataSource.setup(apiKey: key)
            }
        }

        registerBackgroundTask()
    }

    private func registerBackgroundTask() {
        BGTaskScheduler.shared.register(
            forTaskWithIdentifier: "com.prof18.feedflow.articlesync",
            using: nil
        ) { task in
            scheduleAppRefresh()

            let completionLock = NSLock()
            var didComplete = false

            func finish(success: Bool) {
                completionLock.lock()
                defer { completionLock.unlock() }
                guard !didComplete else { return }
                didComplete = true
                task.setTaskCompleted(success: success)
            }

            let backgroundTask = Task {
                let repo = Deps.shared.getSerialFeedFetcherRepository()
                var fetchSucceeded = false

                // Phase 1: Fetch feeds (expensive, may be interrupted)
                do {
                    try await repo.fetchFeeds()
                    fetchSucceeded = true
                } catch is CancellationError {
                    // System cancelled — items carry over to next run
                    finish(success: false)
                    return
                } catch {
                    // Non-cancellation error — partial results may exist, continue to notifications
                }

                WidgetCenter.shared.reloadAllTimelines()

                // Phase 2: Send notifications for whatever was fetched
                if !Task.isCancelled {
                    let authStatus = await UNUserNotificationCenter.current().notificationSettings()
                    if authStatus.authorizationStatus == .authorized || authStatus.authorizationStatus == .provisional {
                        do {
                            let itemsToNotify = try await repo.getFeedSourceToNotify()
                            if !itemsToNotify.isEmpty {
                                let notifier = Deps.shared.getNotifier()
                                let hasShown = notifier.showNewArticlesNotification(feedSourcesToNotify: itemsToNotify)
                                if hasShown {
                                    try await repo.markItemsAsNotified()
                                }
                            }
                        } catch {
                            // Best effort — items will be picked up next run
                        }
                    }
                }

                finish(success: fetchSucceeded)
            }

            task.expirationHandler = {
                backgroundTask.cancel()
                finish(success: false)
            }
        }
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environment(appState)
                .environment(browserSelector)
                .toggleStyle(BlueToggleStyle())
                .preferredColorScheme(appState.colorScheme)
                .onOpenURL { url in
                    if url.scheme == "feedflow" {
                        handleFeedFlowURL(url)
                    } else {
                        Deps.shared.getDropboxDataSource().handleOAuthResponse {
                            DropboxDataSourceIos.handleOAuthResponse(
                                url: url,
                                onSuccess: {
                                    print("Success! User is logged into DropboxClientsManager.")
                                    NotificationCenter.default.post(
                                        name: .didDropboxSuccess, object: nil
                                    )
                                },
                                onCancel: {
                                    print("Authorization flow was manually canceled by user!")
                                    NotificationCenter.default.post(
                                        name: .didDropboxCancel, object: nil
                                    )
                                },
                                onError: {
                                    print("Error")
                                    NotificationCenter.default.post(
                                        name: .didDropboxError, object: nil
                                    )
                                }
                            )
                        }
                    }
                }
                .onChange(of: scenePhase) {
                    switch scenePhase {
                    case .active:
                        feedSyncTimer.scheduleTimer()
                        Deps.shared.getContentPrefetchManager().startBackgroundFetching()
                    case .background:
                        feedSyncTimer.invalidate()
                        scheduleAppRefresh()
                        WidgetCenter.shared.reloadAllTimelines()
                    default:
                        break
                    }
                }
        }
        .commands {
            appMenu
        }
    }

    private func handleFeedFlowURL(_ url: URL) {
        let components = URLComponents(url: url, resolvingAgainstBaseURL: false)
        let path = components?.path ?? ""

        let feedId = path.replacing("/", with: "")
        appState.navigate(route: CommonViewRoute.deepLinkFeed(feedId))
    }
}

func scheduleAppRefresh() {
    let request = BGProcessingTaskRequest(identifier: "com.prof18.feedflow.articlesync")
    // Schedule for 2 hours from now
    request.earliestBeginDate = .now.addingTimeInterval(2 * 3_600)
    request.requiresNetworkConnectivity = true
    try? BGTaskScheduler.shared.submit(request)
}

func setupTelemetry() {
    #if !DEBUG
        let config = TelemetryDeck.Config(appID: "0334762E-7A84-4A80-A1BA-879165ED0333")
        TelemetryDeck.initialize(config: config)
    #endif
}

class AppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate {
    func application(
        _: UIApplication,
        didFinishLaunchingWithOptions _: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        UNUserNotificationCenter.current().delegate = self

        #if !DEBUG
            configureFirebase()
        #endif

        return true
    }

    func userNotificationCenter(
        _: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        let userInfo = response.notification.request.content.userInfo
        if let urlString = userInfo["url"] as? String {
            NotificationCenter.default.post(
                name: .didReceiveNotificationDeepLink,
                object: nil,
                userInfo: ["url": urlString]
            )
        }
        completionHandler()
    }

    func userNotificationCenter(
        _: UNUserNotificationCenter,
        willPresent _: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        completionHandler([.banner, .sound])
    }

    func applicationDidReceiveMemoryWarning(_: UIApplication) {
        Deps.shared.getContentPrefetchManager().pauseFetching()
    }

    func application(
        _: UIApplication,
        handleEventsForBackgroundURLSession identifier: String,
        completionHandler: @escaping () -> Void
    ) {
        DropboxClientsManager.handleEventsForBackgroundURLSession(
            with: identifier,
            creationInfos: [],
            completionHandler: completionHandler
        ) { requestResults in
            DropboxDataSourceIos.processReconnect(requestResults: requestResults)
        }
    }

    private func configureFirebase() {
        #if DEBUG
            let fileName = "GoogleService-Info-dev"
        #else
            let fileName = "GoogleService-Info"
        #endif

        guard let filePath = Bundle.main.path(forResource: fileName, ofType: "plist") else {
            return
        }
        guard let options = FirebaseOptions(contentsOfFile: filePath) else {
            return
        }
        FirebaseApp.configure(options: options)
        #if !DEBUG
            setupCrashlytics()
        #endif
    }
}
