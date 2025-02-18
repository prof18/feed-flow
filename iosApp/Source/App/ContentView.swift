import FeedFlowKit
import SwiftUI

struct ContentView: View {
    @Environment(AppState.self) private var appState
    @Environment(\.scenePhase) private var scenePhase: ScenePhase
    @Environment(\.horizontalSizeClass) private var horizontalSizeClass: UserInterfaceSizeClass?

    @State var browserSelector: BrowserSelector = .init()
    @StateObject private var vmStoreOwner = VMStoreOwner<HomeViewModel>(Deps.shared.getHomeViewModel())

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
    }
}
