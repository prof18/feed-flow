import SwiftUI
import shared

@main
struct iOSApp: App {
    
    init() {
        startKoin()
    }
    
	var body: some Scene {
		WindowGroup {
			ContentView()
		}
	}
}
