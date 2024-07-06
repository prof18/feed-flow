import SwiftUI
import shared
import KMPNativeCoroutinesAsync

struct ContentView: View {

    @EnvironmentObject var appState: AppState
    @EnvironmentObject var browserSelector: BrowserSelector

    @Environment(\.scenePhase) private var scenePhase: ScenePhase
    @Environment(\.horizontalSizeClass) private var horizontalSizeClass: UserInterfaceSizeClass?

    @StateObject var homeViewModel = KotlinDependencies.shared.getHomeViewModel()

    @State private var isAppInBackground: Bool = false

    var body: some View {
        ZStack {
            HomeContainer()
                .environmentObject(appState)
                .environmentObject(browserSelector)

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
                KotlinDependencies.shared.getFeedSyncRepository().enqueueBackup(forceBackup: false)
            default:
                break
            }
        }
    }
}

private struct HomeContainer: View {

    @EnvironmentObject var appState: AppState

    @StateObject var homeViewModel = KotlinDependencies.shared.getHomeViewModel()

    @State private var selectedDrawerItem: DrawerItem? = DrawerItem.Timeline()

    var body: some View {
        if appState.sizeClass == .compact {
            CompactView(selectedDrawerItem: $selectedDrawerItem, homeViewModel: homeViewModel)
        } else {
            RegularView(selectedDrawerItem: $selectedDrawerItem, homeViewModel: homeViewModel)
        }
    }
}

#Preview {
    HomeContainer()
        .environmentObject(AppState())
}
