import SwiftUI
import FeedFlowKit

struct ContentView: View {

    @EnvironmentObject var appState: AppState
    @EnvironmentObject var browserSelector: BrowserSelector

    @Environment(\.scenePhase) private var scenePhase: ScenePhase
    @Environment(\.horizontalSizeClass) private var horizontalSizeClass: UserInterfaceSizeClass?

    @StateObject private var vmStoreOwner = VMStoreOwner<HomeViewModel>(Deps.shared.getHomeViewModel())

    @State private var isAppInBackground: Bool = false

    @State private var selectedDrawerItem: DrawerItem? = DrawerItem.Timeline()

    var body: some View {
        ZStack {
            if appState.sizeClass == .compact {
                CompactView(
                    selectedDrawerItem: $selectedDrawerItem,
                    indexHolder: HomeListIndexHolder(homeViewModel: vmStoreOwner.instance),
                    homeViewModel: vmStoreOwner.instance
                )
                .environmentObject(appState)
                .environmentObject(browserSelector)
            } else {
                RegularView(
                    selectedDrawerItem: $selectedDrawerItem,
                    indexHolder: HomeListIndexHolder(homeViewModel: vmStoreOwner.instance),
                    homeViewModel: vmStoreOwner.instance
                )
                .environmentObject(appState)
                .environmentObject(browserSelector)
            }

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
        .onChange(of: self.horizontalSizeClass) { newSizeClass in
            if !isAppInBackground && newSizeClass != appState.sizeClass {
                appState.sizeClass = newSizeClass
            }
        }
        .onChange(of: scenePhase) { newScenePhase in
            switch newScenePhase {
            case.active:
                isAppInBackground = false
            case .background:
                isAppInBackground = true
            default:
                break
            }
        }
    }
}
