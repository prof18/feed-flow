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
    @Environment(\.horizontalSizeClass)
    private var horizontalSizeClass: UserInterfaceSizeClass?

    @StateObject private var vmStoreOwner = VMStoreOwner<HomeViewModel>(Deps.shared.getHomeViewModel())
    @StateObject private var reviewVmStoreOwner = VMStoreOwner<ReviewViewModel>(Deps.shared.getReviewViewModel())
    @StateObject private var readerModeVmStoreOwner = VMStoreOwner<ReaderModeViewModel>(
        Deps.shared.getReaderModeViewModel())

    @State private var hasTriggeredLaunch = false
    @State private var openingAudioEpisode = false

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
        @Bindable var appState = appState
        let effectiveSizeClass = appState.sizeClass ?? horizontalSizeClass

        Group {
            if effectiveSizeClass == .compact {
                CompactView(
                    selectedSidebarItem: $selectedSidebarItem,
                    homeViewModel: vmStoreOwner.instance,
                    readerModeViewModel: readerModeVmStoreOwner.instance,
                    onOpenAudioEpisode: openAudioEpisode
                )
            } else {
                RegularView(
                    selectedSidebarItem: $selectedSidebarItem,
                    homeViewModel: vmStoreOwner.instance,
                    readerModeViewModel: readerModeVmStoreOwner.instance,
                    onOpenAudioEpisode: openAudioEpisode
                )
            }
        }
        .onAppear {
            if appState.sizeClass == nil {
                appState.sizeClass = horizontalSizeClass
            }
            let savedThemeMode = vmStoreOwner.instance.getCurrentThemeMode()
            appState.updateTheme(savedThemeMode)
        }
        .onChange(of: horizontalSizeClass) {
            synchronizeSizeClassIfActive()
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
        .task(id: scenePhase) {
            guard scenePhase == .active else { return }
            await Task.yield()
            synchronizeSizeClassIfActive()
        }
        .task {
            for await state in reviewVmStoreOwner.instance.canShowReviewDialog {
                let showReview = state as? Bool ?? false
                if showReview {
                    guard let currentScene = UIApplication.shared.connectedScenes.first as? UIWindowScene else {
                        return
                    }
                    AppStore.requestReview(in: currentScene)
                    reviewVmStoreOwner.instance.onReviewShown()
                }
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
        appState.currentCommonRoute = nil
        appState.regularNavigationPath = NavigationPath()
        appState.compactNavigationPath = NavigationPath()
        appState.compactNavigationPath.append(CompactViewRoute.feed)
    }

    private func synchronizeSizeClassIfActive() {
        // Replacing the navigation root while iPadOS detaches the scene can move one navigation item between bars.
        guard scenePhase == .active, horizontalSizeClass != appState.sizeClass else { return }
        preserveVisibleRoute(from: appState.sizeClass, to: horizontalSizeClass)
        appState.sizeClass = horizontalSizeClass
    }

    private func preserveVisibleRoute(
        from oldSizeClass: UserInterfaceSizeClass?,
        to newSizeClass: UserInterfaceSizeClass?
    ) {
        guard let currentCommonRoute = appState.currentCommonRoute else { return }

        if oldSizeClass == .compact, newSizeClass != .compact {
            appState.regularNavigationPath = NavigationPath()
            appState.regularNavigationPath.append(currentCommonRoute)
        } else if oldSizeClass != .compact, newSizeClass == .compact {
            appState.compactNavigationPath = NavigationPath()
            appState.compactNavigationPath.append(CompactViewRoute.feed)
            appState.compactNavigationPath.append(currentCommonRoute)
        }
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
