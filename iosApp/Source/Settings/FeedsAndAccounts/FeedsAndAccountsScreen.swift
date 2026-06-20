import FeedFlowKit
import SwiftUI

struct FeedsAndAccountsScreen: View {
    @Environment(AppState.self)
    private var appState
    @Environment(BrowserSelector.self)
    private var browserSelector

    private let feedFlowStrings = Deps.shared.getStrings()
    let fetchFeeds: () -> Void

    var body: some View {
        @Bindable var appState = appState

        Form {
            Section {
                NavigationLink(destination: FeedSourceListScreen()
                    .environment(browserSelector)) {
                    Label(feedFlowStrings.feedsTitle, systemImage: "list.bullet.rectangle.portrait")
                }
                .accessibilityIdentifier(FeedSourceListAccessibilityIdentifiers.settingsRow)

                NavigationLink(destination: AddFeedScreen()
                    .environment(browserSelector)) {
                    Label(feedFlowStrings.addFeed, systemImage: "plus.circle")
                }

                NavigationLink(destination: ImportExportScreen(fetchFeeds: fetchFeeds)) {
                    Label(feedFlowStrings.importExportLabel, systemImage: "arrow.up.arrow.down")
                }
                .accessibilityIdentifier(FeedsAndAccountsAccessibilityIdentifiers.importExportRow)

                NavigationLink(destination: AccountsScreen()) {
                    Label(feedFlowStrings.settingsAccounts, systemImage: "arrow.triangle.2.circlepath")
                }
                .accessibilityIdentifier(FeedsAndAccountsAccessibilityIdentifiers.accountsRow)

                NavigationLink(destination: NotificationsSettingsScreen()) {
                    Label(feedFlowStrings.settingsNotificationsTitle, systemImage: "bell")
                }

                NavigationLink(destination: BlockedWordsScreen()) {
                    Label(feedFlowStrings.settingsBlockedWords, systemImage: "exclamationmark.triangle")
                }
                .accessibilityIdentifier(FeedsAndAccountsAccessibilityIdentifiers.blockedWordsRow)
            }
        }
        .scrollContentBackground(.hidden)
        .background(Color.secondaryBackgroundColor)
        .navigationTitle(Text(feedFlowStrings.settingsFeedsAndAccounts))
        .navigationBarTitleDisplayMode(.inline)
        .snackbar(messageQueue: $appState.snackbarQueue)
    }
}
