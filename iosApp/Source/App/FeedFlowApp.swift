import SwiftUI
import shared
import FirebaseCore

@main
struct FeedFlowApp: App {

    @UIApplicationDelegateAdaptor(AppDelegate.self) var delegate
    @StateObject var appState: AppState = AppState()
    @StateObject var browserSelector: BrowserSelector = BrowserSelector()

    init() {
    #if !DEBUG
        CrashlyticsKt.setupCrashlytics()
    #endif
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

class AppDelegate: NSObject, UIApplicationDelegate {
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
    #if !DEBUG
        FirebaseApp.configure()
    #endif
        return true
    }
}
