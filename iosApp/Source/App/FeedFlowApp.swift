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

        if let path = Bundle.main.path(forResource: "Info", ofType: "plist") {
            if let keys = NSDictionary(contentsOfFile: path) {
                let key = keys["DropboxApiKey"] as? String ?? ""
                let dropboxDataSource = KotlinDependencies.shared.getDropboxDataSource()
                dropboxDataSource.setup(apiKey: key)
            }
        }

    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(appState)
                .environmentObject(browserSelector)
                .onOpenURL(perform: { url in
                    KotlinDependencies.shared.getDropboxDataSource().handleOAuthResponse {
                        DropboxDataSourceIos.handleOAuthResponse(
                                url: url,
                                onSuccess: {
                                    print("Success! User is logged into DropboxClientsManager.")
                                    NotificationCenter.default.post(name: .didDropboxSuccess, object: nil)
                                },
                                onCancel: {
                                    print("Authorization flow was manually canceled by user!")
                                    NotificationCenter.default.post(name: .didDropboxCancel, object: nil)
                                },
                                onError: {
                                    print("Error")
                                    NotificationCenter.default.post(name: .didDropboxError, object: nil)
                                }
                        )
                    }
                })
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
