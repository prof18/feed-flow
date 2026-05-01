import FeedFlowKit
import SwiftUI

struct SettingsScreen: View {
    @Environment(AppState.self)
    private var appState
    @Environment(\.dismiss)
    private var dismiss
    private let feedFlowStrings = Deps.shared.getStrings()

    let fetchFeeds: () -> Void

    var body: some View {
        NavigationStack {
            @Bindable var appState = appState

            Form {
                Section {
                    NavigationLink(destination: AppearanceScreen()) {
                        Label(feedFlowStrings.settingsAppearance, systemImage: "paintbrush")
                    }

                    NavigationLink(destination: FeedsAndAccountsScreen(
                        fetchFeeds: fetchFeeds
                    )) {
                        Label(feedFlowStrings.settingsFeedsAndAccounts, systemImage: "arrow.triangle.2.circlepath")
                    }

                    NavigationLink(destination: FeedListSettingsScreen()) {
                        Label(feedFlowStrings.settingsFeedListTitle, systemImage: "list.bullet.rectangle.portrait")
                    }

                    NavigationLink(destination: ReadingBehaviorScreen()) {
                        Label(feedFlowStrings.settingsReadingBehavior, systemImage: "book")
                    }

                    NavigationLink(destination: SyncAndStorageScreen()) {
                        Label(feedFlowStrings.settingsSyncAndStorage, systemImage: "externaldrive")
                    }

                    NavigationLink(destination: AboutAndSupportScreen()) {
                        Label(feedFlowStrings.settingsAboutAndSupport, systemImage: "info.circle")
                    }
                } footer: {
                    if let appVersion = Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String {
                        Text(feedFlowStrings.aboutAppVersion(appVersion))
                            .frame(maxWidth: .infinity, alignment: .center)
                            .padding(.vertical, Spacing.small)
                    }
                }
            }
            .scrollContentBackground(.hidden)
            .toolbar {
                Button {
                    dismiss()
                } label: {
                    Text(feedFlowStrings.actionDone).bold()
                }
            }
            .navigationTitle(Text(feedFlowStrings.settingsTitle))
            .navigationBarTitleDisplayMode(.inline)
            .background(Color.secondaryBackgroundColor)
        }
    }
}
