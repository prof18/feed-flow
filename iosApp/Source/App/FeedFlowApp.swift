import SwiftUI
import shared

@main
struct FeedFlowApp: App {
    
    @StateObject var appState: AppState = AppState()
    
    init() {
        startKoin()
    }
    
	var body: some Scene {
		WindowGroup {
			ContentView()
                .environmentObject(appState)
		}
	}
}
