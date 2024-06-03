import SwiftUI
import shared
import FirebaseCore

@main
struct FeedFlowApp: App {

    @UIApplicationDelegateAdaptor(AppDelegate.self) private var delegate

    @StateObject private var appState: AppState = AppState()
    @StateObject private var browserSelector: BrowserSelector = BrowserSelector()

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
        configureFirebase()
    #endif
        return true
    }

    private func configureFirebase() {
        #if DEBUG
            let fileName = "GoogleService-Info-dev"
        #else
            let fileName = "GoogleService-Info"
        #endif

        guard let filePath = Bundle.main.path(forResource: fileName, ofType: "plist") else {
            return
        }
        guard let options = FirebaseOptions(contentsOfFile: filePath) else {
            return
        }
        FirebaseApp.configure(options: options)
    }
}
