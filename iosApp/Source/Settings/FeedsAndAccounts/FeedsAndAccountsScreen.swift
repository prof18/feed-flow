import FeedFlowKit
import SwiftUI

struct FeedsAndAccountsScreen: View {
    @Environment(AppState.self)
    private var appState

    private let feedFlowStrings = Deps.shared.getStrings()
    let fetchFeeds: () -> Void

    var body: some View {
        @Bindable var appState = appState

        Form {
            Section {
                NavigationLink(destination: FeedSourceListScreen()) {
                    Label(feedFlowStrings.feedsTitle, systemImage: "list.bullet.rectangle.portrait")
                }

                NavigationLink(destination: AddFeedScreen()) {
                    Label(feedFlowStrings.addFeed, systemImage: "plus.circle")
                }

                NavigationLink(destination: ImportExportScreen(fetchFeeds: fetchFeeds)) {
                    Label(feedFlowStrings.importExportLabel, systemImage: "arrow.up.arrow.down")
                }

                NavigationLink(destination: AccountsScreen()) {
                    Label(feedFlowStrings.settingsAccounts, systemImage: "arrow.triangle.2.circlepath")
                }

                NavigationLink(destination: NotificationsSettingsScreen()) {
                    Label(feedFlowStrings.settingsNotificationsTitle, systemImage: "bell")
                }

                NavigationLink(destination: BlockedWordsScreen()) {
                    Label(feedFlowStrings.settingsBlockedWords, systemImage: "exclamationmark.triangle")
                }
            }
        }
        .scrollContentBackground(.hidden)
        .background(Color.secondaryBackgroundColor)
        .navigationTitle(Text(feedFlowStrings.settingsFeedsAndAccounts))
        .navigationBarTitleDisplayMode(.inline)
        .snackbar(messageQueue: $appState.snackbarQueue)
    }
}
