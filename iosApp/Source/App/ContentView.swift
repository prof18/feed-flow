import FeedFlowKit
import StoreKit
import SwiftUI

struct ContentView: View {
    @Environment(AppState.self)
    private var appState
    @Environment(\.scenePhase)
    private var scenePhase: ScenePhase
    @Environment(\.horizontalSizeClass)
    private var horizontalSizeClass: UserInterfaceSizeClass?

    @StateObject private var vmStoreOwner = VMStoreOwner<HomeViewModel>(Deps.shared.getHomeViewModel())
    @StateObject private var reviewVmStoreOwner = VMStoreOwner<ReviewViewModel>(Deps.shared.getReviewViewModel())
    @StateObject private var readerModeVmStoreOwner = VMStoreOwner<ReaderModeViewModel>(
        Deps.shared.getReaderModeViewModel())

    @State private var isAppInBackground = false
    @State private var hasTriggeredLaunch = false

    @State private var selectedSidebarItem: SidebarSelection? = .timeline
    @State private var navDrawerState: NavDrawerState = .init(
        timeline: [],
        read: [],
        bookmarks: [],
        categories: [],
        pinnedFeedSources: [],
        feedSourcesWithoutCategory: [],
        feedSourcesByCategory: [:]
    )
    @State private var pendingNotificationSelection: NotificationSelectionTarget?

    var body: some View {
        @Bindable var appState = appState
        let effectiveSizeClass = appState.sizeClass ?? horizontalSizeClass

        Group {
            if effectiveSizeClass == .compact {
                CompactView(
                    selectedSidebarItem: $selectedSidebarItem,
                    indexHolder: HomeListIndexHolder(homeViewModel: vmStoreOwner.instance),
                    homeViewModel: vmStoreOwner.instance,
                    readerModeViewModel: readerModeVmStoreOwner.instance
                )
            } else {
                RegularView(
                    selectedSidebarItem: $selectedSidebarItem,
                    indexHolder: HomeListIndexHolder(homeViewModel: vmStoreOwner.instance),
                    homeViewModel: vmStoreOwner.instance,
                    readerModeViewModel: readerModeVmStoreOwner.instance
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
            if !isAppInBackground && horizontalSizeClass != appState.sizeClass {
                preserveVisibleRoute(from: appState.sizeClass, to: horizontalSizeClass)
                appState.sizeClass = horizontalSizeClass
            }
        }
        .onChange(of: scenePhase) {
            switch scenePhase {
            case .active:
                isAppInBackground = false
                if !hasTriggeredLaunch {
                    hasTriggeredLaunch = true
                    vmStoreOwner.instance.onAppLaunch()
                }
            case .background:
                isAppInBackground = true
            default:
                break
            }
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

    private func showFeedScreen() {
        appState.currentCommonRoute = nil
        appState.regularNavigationPath = NavigationPath()
        appState.compactNavigationPath = NavigationPath()
        appState.compactNavigationPath.append(CompactViewRoute.feed)
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
