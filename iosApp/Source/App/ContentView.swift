import FeedFlowKit
import StoreKit
import SwiftUI

struct ContentView: View {
    @Environment(AppState.self)
    private var appState
    @Environment(AudioPlaybackController.self)
    private var audioPlayback
    @Environment(BrowserSelector.self)
    private var browserSelector
    @Environment(\.openURL)
    private var openURL
    @Environment(\.scenePhase)
    private var scenePhase: ScenePhase
    @Environment(\.requestReview)
    private var requestReview

    @StateObject private var vmStoreOwner = VMStoreOwner<HomeViewModel>(Deps.shared.getHomeViewModel())
    @StateObject private var reviewVmStoreOwner = VMStoreOwner<ReviewViewModel>(Deps.shared.getReviewViewModel())
    @StateObject private var readerModeVmStoreOwner = VMStoreOwner<ReaderModeViewModel>(
        Deps.shared.getReaderModeViewModel()
    )
    @StateObject private var refreshVerificationVmStoreOwner = VMStoreOwner<FeedRefreshVerificationViewModel>(
        Deps.shared.getFeedRefreshVerificationViewModel()
    )

    @State private var hasTriggeredLaunch = false
    @State private var openingAudioEpisode = false
    @State private var isAppForeground = false
    @State private var wasInBackground = true
    @State private var visibleWebsiteVerificationRequest: WebsiteVerificationRequest?
    @State private var pendingForegroundVerification: PendingFeedVerification?
    @State private var foregroundVerificationExplanationURL: String?
    private let isWebsiteVerificationAvailable = Deps.shared.getFeedRecoveryCoordinator().available

    @State private var selectedSidebarItem: SidebarSelection? = .timeline
    @State private var navDrawerState: NavDrawerState = .init(
        timeline: [],
        read: [],
        bookmarks: [],
        categories: [],
        pinnedFeedSources: [],
        feedSourcesWithoutCategory: [],
        feedSourcesByCategory: [:],
        uncategorizedPosition: 0
    )
    @State private var pendingNotificationSelection: NotificationSelectionTarget?

    var body: some View {
        RegularView(
            selectedSidebarItem: $selectedSidebarItem,
            homeViewModel: vmStoreOwner.instance,
            readerModeViewModel: readerModeVmStoreOwner.instance,
            onOpenAudioEpisode: openAudioEpisode
        )
        .websiteVerificationExplanation(
            url: foregroundVerificationExplanationURL,
            identity: foregroundVerificationIdentity,
            onCancel: {
                guard let pendingForegroundVerification else { return }
                refreshVerificationVmStoreOwner.instance.dismiss(pending: pendingForegroundVerification)
            },
            onConfirm: {
                guard let pendingForegroundVerification else { return }
                refreshVerificationVmStoreOwner.instance.verify(pending: pendingForegroundVerification)
            }
        )
        .onChange(of: scenePhase, initial: true) { _, newPhase in
            isAppForeground = newPhase == .active
            switch newPhase {
            case .active:
                if wasInBackground {
                    wasInBackground = false
                    refreshVerificationVmStoreOwner.instance.onForeground()
                }
                updateForegroundVerificationExplanation()
                WebsiteVerificationPresenter.shared.update(
                    sessionID: visibleWebsiteVerificationRequest?.sessionId
                )
            case .background:
                wasInBackground = true
                foregroundVerificationExplanationURL = nil
                visibleWebsiteVerificationRequest = nil
                guard !WebsiteVerificationPresenter.shared.hasForegroundActiveScene else { break }
                WebsiteVerificationPresenter.shared.update(sessionID: nil)
                refreshVerificationVmStoreOwner.instance.cancel()
                Task {
                    try? await Deps.shared.getFeedRecoveryCoordinator().cancelAll()
                }
            default:
                break
            }
        }
        .onAppear {
            let savedThemeMode = vmStoreOwner.instance.getCurrentThemeMode()
            appState.updateTheme(savedThemeMode)
        }
        .onChange(of: scenePhase) {
            switch scenePhase {
            case .active:
                if !hasTriggeredLaunch {
                    hasTriggeredLaunch = true
                    vmStoreOwner.instance.onAppLaunch()
                }
            default:
                break
            }
        }
        .task {
            for await state in reviewVmStoreOwner.instance.canShowReviewDialog {
                let showReview = state as? Bool ?? false
                if showReview {
                    requestReview()
                    reviewVmStoreOwner.instance.onReviewShown()
                }
            }
        }
        .task {
            for await request in Deps.shared.getFeedRecoveryCoordinator().visibleState {
                visibleWebsiteVerificationRequest = request
                guard isAppForeground else { continue }
                WebsiteVerificationPresenter.shared.update(sessionID: request?.sessionId)
            }
        }
        .task {
            for await failure in Deps.shared.getFeedRecoveryCoordinator().failures {
                appState.snackbarQueue.append(
                    SnackbarData(title: Self.message(for: failure), subtitle: nil, showBanner: true)
                )
            }
        }
        .task {
            for await pending in refreshVerificationVmStoreOwner.instance.pending {
                pendingForegroundVerification = pending.first
                updateForegroundVerificationExplanation()
            }
        }
        .task {
            var previousEpoch: String?
            for await epoch in Deps.shared.getFeedRecoveryRepository().accountEpochState {
                if let previousEpoch, previousEpoch != epoch {
                    refreshVerificationVmStoreOwner.instance.cancel()
                    pendingForegroundVerification = nil
                    foregroundVerificationExplanationURL = nil
                    try? await Deps.shared.getFeedRecoveryCoordinator().cancelAll()
                }
                previousEpoch = epoch
            }
        }
        .task {
            for await state in vmStoreOwner.instance.navDrawerState {
                navDrawerState = state
                if let target = pendingNotificationSelection {
                    selectedSidebarItem = sidebarSelection(for: target)
                    pendingNotificationSelection = nil
                }
            }
        }
        .onReceive(NotificationCenter.default.publisher(for: .didReceiveNotificationDeepLink)) { notification in
            guard let urlString = notification.userInfo?["url"] as? String,
                  let url = URL(string: urlString),
                  url.scheme == "feedflow" else { return }
            handleFeedFlowNotificationURL(url)
        }
        #if DEBUG
            .overlay(alignment: .bottom) {
                if let e2eSeedMessage = appState.e2eSeedMessage {
                    Text(e2eSeedMessage)
                        .font(.headline)
                        .padding(.horizontal, 16)
                        .padding(.vertical, 10)
                        .background(.regularMaterial, in: Capsule())
                        .padding(.bottom, 18)
                        .onTapGesture {
                            appState.e2eSeedMessage = nil
                        }
                        .accessibilityIdentifier(
                            e2eSeedMessage == "E2E seed complete" ? "e2e_seed_complete" : "e2e_seed_error"
                        )
                }
            }
        #endif
    }
}

private extension ContentView {
    private func updateForegroundVerificationExplanation() {
        guard isAppForeground else { return }
        guard isWebsiteVerificationAvailable,
              let pendingForegroundVerification
        else {
            foregroundVerificationExplanationURL = nil
            return
        }
        foregroundVerificationExplanationURL = pendingForegroundVerification.record.effectiveUrl
    }

    private var foregroundVerificationIdentity: String? {
        guard let record = pendingForegroundVerification?.record else { return nil }
        return [record.accountEpoch, record.sourceId, record.subscriptionUrl, record.effectiveUrl]
            .joined(separator: "|")
    }

    private static func message(for failure: VerificationFailure) -> String {
        switch failure {
        case .saveFailed:
            feedFlowStrings.feedVerificationSaveFailed
        case .browserStopped:
            feedFlowStrings.feedVerificationBrowserFailed
        case .unsupported:
            feedFlowStrings.feedVerificationUnsupported
        default:
            feedFlowStrings.feedVerificationFailed
        }
    }

    private func handleFeedFlowNotificationURL(_ url: URL) {
        let host = url.host ?? ""
        let pathComponents = url.pathComponents.filter { $0 != "/" }

        switch host {
        case "feedsourcefilter":
            if let feedSourceId = pathComponents.first {
                showFeedScreen()
                let target = NotificationSelectionTarget.feedSource(feedSourceId)
                pendingNotificationSelection = target
                selectedSidebarItem = sidebarSelection(for: target)
                vmStoreOwner.instance.updateFeedSourceFilter(feedSourceId: feedSourceId)
            }
        case "category":
            if let categoryId = pathComponents.first {
                showFeedScreen()
                let target = NotificationSelectionTarget.category(categoryId)
                pendingNotificationSelection = target
                selectedSidebarItem = sidebarSelection(for: target)
                vmStoreOwner.instance.updateCategoryFilter(categoryId: categoryId)
            }
        default:
            break
        }
    }

    private func openAudioEpisode(_ episode: AudioEpisode) {
        guard !openingAudioEpisode else { return }
        openingAudioEpisode = true
        Task { @MainActor in
            defer { openingAudioEpisode = false }
            do {
                let info = try await readerModeVmStoreOwner.instance.getAudioEpisodeReaderInfo(
                    feedItemId: FeedItemId(id: episode.itemId)
                )
                guard audioPlayback.episode?.itemId == episode.itemId else { return }
                guard let info else {
                    appState.emitGenericError()
                    return
                }
                let openMode = browserSelector.resolvedOpenMode(for: info)
                switch openMode {
                case .fullArticle, .feedContent:
                    readerModeVmStoreOwner.instance.loadReaderContent(urlInfo: info)
                    appState.navigate(route: CommonViewRoute.readerMode)
                default:
                    guard let url = URL(string: info.url) else {
                        appState.emitGenericError()
                        return
                    }
                    if openMode == .internalBrowser || browserSelector.openInAppBrowser(),
                       browserSelector.isValidForInAppBrowser(url) {
                        appState.openInAppBrowser(url: url)
                    } else {
                        openURL(browserSelector.getUrlForDefaultBrowser(stringUrl: info.url))
                    }
                }
            } catch is CancellationError {
                return
            } catch {
                appState.emitGenericError()
            }
        }
    }

    private func showFeedScreen() {
        appState.regularNavigationPath = NavigationPath()
    }

    private func sidebarSelection(for target: NotificationSelectionTarget) -> SidebarSelection {
        switch target {
        case let .feedSource(feedSourceId):
            return .feedSource(id: feedSourceId)
        case let .category(categoryId):
            return .category(id: categoryId)
        }
    }
}

private enum NotificationSelectionTarget {
    case feedSource(String)
    case category(String)
}
