import FeedFlowKit
import SwiftUI

#if !DEBUG
    import FirebaseCrashlytics
#endif

struct AboutAndSupportScreen: View {
    @Environment(AppState.self) 
    private var appState
    
    @Environment(\.openURL) 
    private var openURL
    
    @StateObject private var vmStoreOwner = VMStoreOwner<AboutAndSupportSettingsViewModel>(
        Deps.shared.getAboutAndSupportSettingsViewModel()
    )
    private let feedFlowStrings = Deps.shared.getStrings()

    @State private var settingsState = AboutAndSupportState(
        isCrashReportingEnabled: true
    )

    var body: some View {
        @Bindable var appState = appState

        AboutAndSupportScreenContent(
            isCrashReportingEnabled: Binding(
                get: { settingsState.isCrashReportingEnabled },
                set: { newValue in
                    vmStoreOwner.instance.updateCrashReporting(value: newValue)
                    #if !DEBUG
                        Crashlytics.crashlytics().setCrashlyticsCollectionEnabled(newValue)
                    #endif
                }
            ),
            openURL: openURL,
            appState: appState,
        )
        .navigationTitle(Text(feedFlowStrings.settingsAboutAndSupport))
        .navigationBarTitleDisplayMode(.inline)
        .snackbar(messageQueue: $appState.snackbarQueue)
        .task {
            for await state in vmStoreOwner.instance.state {
                self.settingsState = state
            }
        }
    }
}
