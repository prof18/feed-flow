import FeedFlowKit
import FirebaseCore
import FirebaseCrashlytics
import SwiftUI
import SwiftyDropbox
import WidgetKit

@main
struct FeedFlowApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) private var delegate
    @Environment(\.scenePhase) private var scenePhase: ScenePhase
    @State private var appState: AppState = .init()

    private var feedSyncTimer: FeedSyncTimer = .init()

    init() {
        #if !DEBUG
            setupCrashlytics()
        #endif
        startKoin()
        #if !DEBUG
            let isCrashReportEnabled = Deps.shared.getSettingsRepository()
                .getCrashReportingEnabled()
            Crashlytics.crashlytics().setCrashlyticsCollectionEnabled(isCrashReportEnabled)
        #endif

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
                .onOpenURL(perform: { url in
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

class AppDelegate: NSObject, UIApplicationDelegate {
    func application(
        _: UIApplication,
        didFinishLaunchingWithOptions _: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        #if !DEBUG
            configureFirebase()
        #endif
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
            completionHandler: completionHandler,
            requestsToReconnect: { requestResults in
                DropboxDataSourceIos.processReconnect(requestResults: requestResults)
            }
        )
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
    }
}
