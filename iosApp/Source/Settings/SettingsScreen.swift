import FeedFlowKit
import SwiftUI

struct SettingsScreen: View {
    @Environment(AppState.self)
    private var appState
    @Environment(\.dismiss)
    private var dismiss
    @Environment(BrowserSelector.self)
    private var browserSelector
    private let feedFlowStrings = Deps.shared.getStrings()

    let fetchFeeds: () -> Void

    var body: some View {
        NavigationStack {
            @Bindable var appState = appState

            Form {
                Section {
                    NavigationLink(destination: FeedSourceListScreen()
                        .environment(browserSelector)) {
                        Label(feedFlowStrings.feedsTitle, systemImage: "dot.radiowaves.up.forward")
                    }
                    .accessibilityIdentifier(FeedSourceListAccessibilityIdentifiers.settingsRow)

                    NavigationLink(destination: AddFeedScreen()
                        .environment(browserSelector)) {
                        Label(feedFlowStrings.addFeed, systemImage: "plus.circle")
                    }

                    NavigationLink(destination: ImportExportScreen(fetchFeeds: fetchFeeds)) {
                        Label(feedFlowStrings.importExportLabel, systemImage: "arrow.up.arrow.down")
                    }
                    .accessibilityIdentifier(SettingsAccessibilityIdentifiers.importExportRow)

                    NavigationLink(destination: BlockedWordsScreen()) {
                        Label(feedFlowStrings.settingsBlockedWords, systemImage: "exclamationmark.triangle")
                    }
                    .accessibilityIdentifier(SettingsAccessibilityIdentifiers.blockedWordsRow)
                } header: {
                    Text(feedFlowStrings.feedsTitle)
                }

                Section {
                    NavigationLink(destination: SyncAndRefreshScreen()) {
                        Label(feedFlowStrings.settingsSyncAndRefresh, systemImage: "arrow.triangle.2.circlepath")
                    }
                    .accessibilityIdentifier(SettingsAccessibilityIdentifiers.syncRow)

                    NavigationLink(destination: NotificationsSettingsScreen()) {
                        Label(feedFlowStrings.settingsNotificationsTitle, systemImage: "bell")
                    }
                    .accessibilityIdentifier(SettingsAccessibilityIdentifiers.notificationsRow)

                    NavigationLink(destination: StorageScreen()) {
                        Label(feedFlowStrings.settingsStorage, systemImage: "externaldrive")
                    }
                    .accessibilityIdentifier(SettingsAccessibilityIdentifiers.storageRow)
                } header: {
                    Text(feedFlowStrings.settingsSyncAndStorage)
                }

                Section {
                    NavigationLink(destination: AppearanceScreen()) {
                        Label(feedFlowStrings.settingsAppearance, systemImage: "paintbrush")
                    }
                    .accessibilityIdentifier(SettingsAccessibilityIdentifiers.appearanceRow)

                    NavigationLink(destination: FeedListSettingsScreen()) {
                        Label(feedFlowStrings.settingsFeedListTitle, systemImage: "list.bullet.rectangle.portrait")
                    }
                    .accessibilityIdentifier(SettingsAccessibilityIdentifiers.feedListRow)

                    NavigationLink(destination: ReadingBehaviorScreen()) {
                        Label(feedFlowStrings.settingsReadingBehavior, systemImage: "book")
                    }
                    .accessibilityIdentifier(SettingsAccessibilityIdentifiers.readingBehaviorRow)
                } header: {
                    Text(feedFlowStrings.settingsSectionDisplayAndReading)
                }

                Section {
                    if RevenueCatSupport.isAvailable {
                        NavigationLink {
                            SupportPaywallScreen()
                        } label: {
                            Label(feedFlowStrings.supportTheProject, systemImage: "heart")
                        }
                        .accessibilityIdentifier(SettingsAccessibilityIdentifiers.supportProjectRow)
                    }

                    NavigationLink(destination: AboutAndSupportScreen()) {
                        Label(feedFlowStrings.settingsAboutAndSupport, systemImage: "info.circle")
                    }
                } header: {
                    Text(feedFlowStrings.aboutNavBar)
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
                .accessibilityIdentifier(SettingsAccessibilityIdentifiers.doneButton)
            }
            .navigationTitle(Text(feedFlowStrings.settingsTitle))
            .navigationBarTitleDisplayMode(.inline)
            .background(Color.secondaryBackgroundColor)
        }
        .toggleStyle(BlueToggleStyle())
    }
}
