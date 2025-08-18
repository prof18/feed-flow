import BackgroundTasks
import FeedFlowKit
import FirebaseCore
import FirebaseCrashlytics
import SwiftUI
import SwiftyDropbox
import TelemetryDeck
import WidgetKit

@main
struct FeedFlowApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self)
    private var delegate
    @Environment(\.scenePhase)
    private var scenePhase: ScenePhase
    @State private var appState: AppState = .init()

    private var feedSyncTimer: FeedSyncTimer = .init()

    init() {
        startKoin()
        setupTelemetry()

        if let path = Bundle.main.path(forResource: "Info", ofType: "plist") {
            if let keys = NSDictionary(contentsOfFile: path) {
                let key = keys["DropboxApiKey"] as? String ?? ""
                let dropboxDataSource = Deps.shared.getDropboxDataSource()
                dropboxDataSource.setup(apiKey: key)
            }
        }
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environment(appState)
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
                })
                .onChange(of: scenePhase) {
                    switch scenePhase {
                    case .active:
                        feedSyncTimer.scheduleTimer()
                    case .background:
                        feedSyncTimer.invalidate()
                        scheduleAppRefresh()
                        WidgetCenter.shared.reloadAllTimelines()
                    default:
                        break
                    }
                }
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
        #if !DEBUG
            configureFirebase()
        #endif

        UNUserNotificationCenter.current().delegate = self

        BGTaskScheduler.shared.register(
            forTaskWithIdentifier: "com.prof18.feedflow.articlesync",
            using: nil
        ) { task in
            scheduleAppRefresh()

            let backgroundTask = Task {
                do {
                    let repo = Deps.shared.getSerialFeedFetcherRepository()
                    try await repo.fetchFeeds()
                    WidgetCenter.shared.reloadAllTimelines()
                    task.setTaskCompleted(success: true)
                } catch {
                    task.setTaskCompleted(success: false)
                }
            }

            task.expirationHandler = {
                backgroundTask.cancel()
                task.setTaskCompleted(success: false)
            }
        }

        return true
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

    func userNotificationCenter(
        _: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        let userInfo = response.notification.request.content.userInfo

        if let feedSourceId = userInfo["feedSourceId"] as? String {
            NotificationCenter.default.post(
                name: .didReceiveNotificationDeepLink,
                object: nil,
                userInfo: ["feedSourceId": feedSourceId]
            )
        }
        completionHandler()
    }
}
