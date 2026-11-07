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
    @State private var appearance: AppAppearance
    @State private var browserSelector: BrowserSelector
    @State private var audioPlayback: AudioPlaybackController

    private var feedSyncTimer: FeedSyncTimer = .init()

    init() {
        SVGImageDecoder.register()
        startKoin(notifier: IOSNotifier(), websiteRecovery: true)
        RevenueCatSupport.configure()
        setupTelemetry()
        _browserSelector = State(initialValue: BrowserSelector())
        _appearance = State(initialValue: AppAppearance())
        _audioPlayback = State(initialValue: AudioPlaybackController(
            positionStore: RepositoryAudioPlaybackPositionStore(
                repository: Deps.shared.getAudioPlaybackPositionRepository()
            ),
            speechText: { title, content in
                try await Deps.shared.getReaderSpeechText().segments(title: title, content: content)
            }
        ))

        if let path = Bundle.main.path(forResource: "Info", ofType: "plist") {
            if let keys = NSDictionary(contentsOfFile: path) {
                let key = keys["DropboxApiKey"] as? String ?? ""
                let dropboxDataSource = Deps.shared.getDropboxDataSource()
                dropboxDataSource.setup(apiKey: key)
            }
        }

        for identifier in BackgroundRefreshScheduler.taskIdentifiers {
            registerBackgroundTask(identifier: identifier)
        }
    }

    // A background launch never reports a .background scene phase, so the live queries are
    // paused here; becoming active resumes them.
    @MainActor
    private func pauseLiveQueriesUnlessActive() {
        if UIApplication.shared.applicationState != .active {
            Deps.shared.getLiveQueryGate().pause()
        }
    }

    private func registerBackgroundTask(identifier: String) {
        BGTaskScheduler.shared.register(
            forTaskWithIdentifier: identifier,
            using: nil
        ) { task in
            BackgroundRefreshScheduler.schedule()

            let completion = BackgroundTaskCompletion { success in
                task.setTaskCompleted(success: success)
            }

            func finish(success: Bool) {
                completion.finish(success: success)
            }

            let backgroundTask = Task {
                await pauseLiveQueriesUnlessActive()
                let repo = Deps.shared.getSerialFeedFetcherRepository()
                var fetchSucceeded = false

                // Phase 1: Fetch feeds (expensive, may be interrupted)
                do {
                    if task is BGAppRefreshTask {
                        try await repo.refreshFeedsQuickly(
                            timeBudgetMillis: BackgroundRefreshScheduler.refreshTimeBudgetMillis
                        )
                    } else {
                        try await repo.fetchFeeds(forceRefresh: false)
                    }
                    fetchSucceeded = true
                } catch is CancellationError {
                    // System cancelled — items carry over to next run. The feeds fetched before the
                    // cutoff are already saved, so the widget can show them now.
                    WidgetCenter.shared.reloadAllTimelines()
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
                        let mode = Deps.shared.getSettingsRepository().getNotificationMode()
                        if mode == .article {
                            do {
                                let articles = Deps.shared.getArticleNotificationRepository()
                                let batch = try await articles.prepareBatch()
                                let notifier = IOSArticleNotifier(strings: feedFlowStrings)
                                try await notifier.showArticleNotifications(batch: batch) { ids in
                                    try await articles.acknowledge(ids: ids)
                                }
                            } catch {
                                finish(success: false)
                                return
                            }
                        } else {
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
                }

                finish(success: fetchSucceeded)
            }

            task.expirationHandler = {
                backgroundTask.cancel()
                completion.finishAfterGracePeriod()
            }
        }
    }

    var body: some Scene {
        WindowGroup {
            FeedFlowScene(appearance: appearance) { appState in
                ContentView()
                .environment(browserSelector)
                .environment(audioPlayback)
                .toggleStyle(BlueToggleStyle())
                .onOpenURL { url in
                    if url.scheme == "feedflow" {
                        handleFeedFlowURL(url, appState: appState)
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
                        Deps.shared.getLiveQueryGate().resume()
                        feedSyncTimer.scheduleTimer()
                        Deps.shared.getContentPrefetchManager().startBackgroundFetching()
                    case .background:
                        // Live queries re-run after every write, outside any suspension guard: one
                        // still reading the app-group database when iOS suspends the app gets it
                        // killed (0xdead10cc). They resume, with fresh data, when the app is active.
                        Deps.shared.getLiveQueryGate().pause()
                        audioPlayback.savePosition()
                        feedSyncTimer.invalidate()
                        // Stop prefetch DB writes before suspension: a SQLite lock held on the
                        // app-group container while suspended gets the app killed (0xdead10cc).
                        Deps.shared.getContentPrefetchManager().pauseFetching()
                        BackgroundRefreshScheduler.schedule()
                        WidgetCenter.shared.reloadAllTimelines()
                    default:
                        break
                    }
                }
            }
        }
        .commands {
            appMenu
        }
    }

    private func handleFeedFlowURL(_ url: URL, appState: AppState) {
        #if DEBUG
            if url.host == "e2e" {
                handleE2eURL(url, appState: appState)
                return
            }
        #endif

        if url.host == "feed" {
            appState.widgetLinks.enqueue(url)
        }
    }

    #if DEBUG
        private func enqueueE2eSnackbars(queryItems: [URLQueryItem], appState: AppState) {
            appState.emitGenericError()
            if queryItems.contains(where: { $0.name == "count" && $0.value == "2" }) {
                appState.emitGenericError()
            }
        }

        private func handleE2eURL(_ url: URL, appState: AppState) {
            let action = url.pathComponents
                .first { $0 != "/" } ?? "reset-and-seed"
            let components = URLComponents(url: url, resolvingAgainstBaseURL: false)
            if action == "snackbar" {
                enqueueE2eSnackbars(queryItems: components?.queryItems ?? [], appState: appState)
                return
            }
            let profileName = components?.queryItems?
                .first { $0.name == "profile" }?
                .value
            let accountName = components?.queryItems?
                .first { $0.name == "account" }?
                .value
            let verificationFixture = components?.queryItems?
                .first { $0.name == "verification" }?
                .value
            // iOS truncates opened URLs to 2047 characters, so a full subscription list does not fit
            // in the link: the restore script copies it into Documents instead.
            let audioUrl = components?.queryItems?
                .first { $0.name == "audioUrl" }?
                .value
            let developmentOpml = components?.queryItems?
                .first { $0.name == "opml" }?
                .value
                .flatMap(decodeDevelopmentOPML) ?? readDevelopmentOPMLFile()
            audioPlayback.stop()
            appState.e2eSeedMessage = nil
            Task {
                do {
                    let seedError = try await Deps.shared.runE2eSeed(
                        action: action,
                        profileName: profileName,
                        accountName: accountName,
                        developmentOpml: developmentOpml,
                        audioUrl: audioUrl,
                        verificationFixture: verificationFixture
                    )
                    await MainActor.run {
                        if let seedError {
                            appState.e2eSeedMessage = "E2E seed failed: \(seedError)"
                        } else {
                            appState.regularNavigationPath = NavigationPath()
                            appState.e2eSeedMessage = "E2E seed complete"
                        }
                    }
                } catch {
                    await MainActor.run {
                        appState.e2eSeedMessage = "E2E seed failed: \(error.localizedDescription)"
                    }
                }
            }
        }
    #endif
}

private func decodeDevelopmentOPML(_ urlSafeBase64: String) -> String? {
    let standardBase64 = urlSafeBase64
        .replacingOccurrences(of: "-", with: "+")
        .replacingOccurrences(of: "_", with: "/")
    let padding = String(repeating: "=", count: (4 - standardBase64.count % 4) % 4)
    guard let data = Data(base64Encoded: standardBase64 + padding) else {
        return nil
    }
    return String(data: data, encoding: .utf8)
}

private func readDevelopmentOPMLFile() -> String? {
    guard let documents = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first else {
        return nil
    }
    let fileURL = documents.appendingPathComponent("e2e-restore/development.opml")
    return try? String(contentsOf: fileURL, encoding: .utf8)
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
        let url = (userInfo["url"] as? String).flatMap(URL.init(string:))
        Task { @MainActor in
            if let url { NotificationDeepLinkStore.shared.enqueue(url) }
            completionHandler()
        }
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
