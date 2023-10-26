import SwiftUI
import shared
import KMPNativeCoroutinesAsync

struct ContentView: View {

    @EnvironmentObject
    var appState: AppState

    @EnvironmentObject
    var browserSelector: BrowserSelector

    @StateObject
    var homeViewModel = KotlinDependencies.shared.getHomeViewModel()

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
    }
}

private struct HomeContainer: View {

    @EnvironmentObject
    var appState: AppState

    @Environment(\.horizontalSizeClass)
    private var horizontalSizeClass

    @StateObject
    var homeViewModel = KotlinDependencies.shared.getHomeViewModel()

    @State
    private var selectedDrawerItem: DrawerItem? = DrawerItem.Timeline()

    var body: some View {
        if horizontalSizeClass == .compact {
            CompactView(selectedDrawerItem: $selectedDrawerItem, homeViewModel: homeViewModel)
        } else {
            RegularView(selectedDrawerItem: $selectedDrawerItem, homeViewModel: homeViewModel)
        }
    }
}
