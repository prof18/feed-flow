import SwiftUI
import shared

@main
struct FeedFlowApp: App {

    @StateObject var appState: AppState = AppState()
    @StateObject var browserSelector: BrowserSelector = BrowserSelector()

    init() {
        startKoin()
    }

	var body: some Scene {
		WindowGroup {
			ContentView()
                .environmentObject(appState)
                .environmentObject(browserSelector)
		}
	}
}
