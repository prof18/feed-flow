import FeedFlowKit
import StoreKit
import SwiftUI

struct ContentView: View {
    @Environment(AppState.self) private var appState
    @Environment(\.scenePhase) private var scenePhase: ScenePhase
    @Environment(\.horizontalSizeClass) private var horizontalSizeClass: UserInterfaceSizeClass?

    @State var browserSelector: BrowserSelector = .init()
    @StateObject private var vmStoreOwner = VMStoreOwner<HomeViewModel>(Deps.shared.getHomeViewModel())
    @StateObject private var reviewVmStoreOwner = VMStoreOwner<ReviewViewModel>(Deps.shared.getReviewViewModel())

    @State private var isAppInBackground: Bool = false

    @State private var selectedDrawerItem: DrawerItem? = DrawerItem.Timeline(unreadCount: 0)

    var body: some View {
        ZStack {
            if appState.sizeClass == .compact {
                CompactView(
                    selectedDrawerItem: $selectedDrawerItem,
                    indexHolder: HomeListIndexHolder(homeViewModel: vmStoreOwner.instance),
                    homeViewModel: vmStoreOwner.instance
                )
                .environment(browserSelector)
            } else {
                RegularView(
                    selectedDrawerItem: $selectedDrawerItem,
                    indexHolder: HomeListIndexHolder(homeViewModel: vmStoreOwner.instance),
                    homeViewModel: vmStoreOwner.instance
                )
                .environment(browserSelector)
            }

            @Bindable var appState = appState
            VStack(spacing: 0) {
                Spacer()

                Snackbar(messageQueue: $appState.snackbarQueue)
            }
        }
        .onAppear {
            if appState.sizeClass == nil {
                appState.sizeClass = horizontalSizeClass
            }
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
        .onReceive(NotificationCenter.default.publisher(for: .didReceiveNotificationDeepLink)) { notification in
            if let feedSourceId = notification.userInfo?["feedSourceId"] as? String {
                vmStoreOwner.instance.updateFeedSourceFilter(feedSourceId: feedSourceId)
            }
        }
    }
}
