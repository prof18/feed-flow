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

    @State var browserSelector: BrowserSelector = .init()
    @StateObject private var vmStoreOwner = VMStoreOwner<HomeViewModel>(Deps.shared.getHomeViewModel())
    @StateObject private var reviewVmStoreOwner = VMStoreOwner<ReviewViewModel>(Deps.shared.getReviewViewModel())
    @StateObject private var readerModeVmStoreOwner = VMStoreOwner<ReaderModeViewModel>(
        Deps.shared.getReaderModeViewModel())

    @State private var isAppInBackground = false

    @State private var selectedDrawerItem: DrawerItem? = DrawerItem.Timeline(unreadCount: 0)

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
                .environment(browserSelector)
            } else {
                RegularView(
                    selectedDrawerItem: $selectedDrawerItem,
                    indexHolder: HomeListIndexHolder(homeViewModel: vmStoreOwner.instance),
                    homeViewModel: vmStoreOwner.instance,
                    readerModeViewModel: readerModeVmStoreOwner.instance
                )
                .environment(browserSelector)
            }
        }
        .snackbar(messageQueue: $appState.snackbarQueue)
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
    }
}
