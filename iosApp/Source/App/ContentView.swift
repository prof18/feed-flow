import SwiftUI
import shared

struct ContentView: View {

    @EnvironmentObject var appState: AppState
    @EnvironmentObject var browserSelector: BrowserSelector

    var body: some View {

        ZStack {
            NavigationStack {
                HomeScreen()
                    .environmentObject(appState)
                    .environmentObject(browserSelector)
                    .navigationDestination(for: Route.self) { route in
                        switch route {
                        case .aboutScreen:
                            AboutScreen()

                        case .importExportScreen:
                            ImportExportScreen()
                        }
                    }
            }

            VStack(spacing: 0) {

                Spacer()

                Snackbar(messageQueue: $appState.snackbarQueue)
            }
        }
    }
}
