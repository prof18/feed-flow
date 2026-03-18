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

    @State private var selectedDrawerItem: DrawerItem? = DrawerItem.Timeline(unreadCount: 0)
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

        Group {
            if appState.sizeClass == .compact {
                CompactView(
                    selectedDrawerItem: $selectedDrawerItem,
                    indexHolder: HomeListIndexHolder(homeViewModel: vmStoreOwner.instance),
                    homeViewModel: vmStoreOwner.instance,
                    readerModeViewModel: readerModeVmStoreOwner.instance
                )
            } else {
                RegularView(
                    selectedDrawerItem: $selectedDrawerItem,
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
                if let target = pendingNotificationSelection,
                   let drawerItem = drawerItem(for: target) {
                    selectedDrawerItem = drawerItem
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
                selectedDrawerItem = drawerItem(for: target)
                vmStoreOwner.instance.updateFeedSourceFilter(feedSourceId: feedSourceId)
            }
        case "category":
            if let categoryId = pathComponents.first {
                showFeedScreen()
                let target = NotificationSelectionTarget.category(categoryId)
                pendingNotificationSelection = target
                selectedDrawerItem = drawerItem(for: target)
                vmStoreOwner.instance.updateCategoryFilter(categoryId: categoryId)
            }
        default:
            break
        }
    }

    private func showFeedScreen() {
        if appState.sizeClass == .compact {
            appState.compatNavigationPath = NavigationPath()
            appState.compatNavigationPath.append(CompactViewRoute.feed)
        } else {
            appState.regularNavigationPath = NavigationPath()
        }
    }

    private func drawerItem(for target: NotificationSelectionTarget) -> DrawerItem? {
        switch target {
        case let .feedSource(feedSourceId):
            return allFeedSourceDrawerItems().first { $0.feedSource.id == feedSourceId }
        case let .category(categoryId):
            return navDrawerState.categories
                .compactMap { $0 as? DrawerItem.DrawerCategory }
                .first { $0.category.id == categoryId }
        }
    }

    private func allFeedSourceDrawerItems() -> [DrawerItem.DrawerFeedSource] {
        let groupedFeedSources = navDrawerState.feedSourcesByCategory.values
            .flatMap { $0 }
            .compactMap { $0 as? DrawerItem.DrawerFeedSource }

        return navDrawerState.pinnedFeedSources
            .compactMap { $0 as? DrawerItem.DrawerFeedSource } +
            navDrawerState.feedSourcesWithoutCategory
            .compactMap { $0 as? DrawerItem.DrawerFeedSource } +
            groupedFeedSources
    }
}

private enum NotificationSelectionTarget {
    case feedSource(String)
    case category(String)
}
