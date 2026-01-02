import FeedFlowKit
import SwiftUI

struct FeedsAndAccountsScreen: View {
    @Environment(AppState.self) 
    private var appState
    
    @Environment(\.dismiss) 
    private var dismiss
    
    private let feedFlowStrings = Deps.shared.getStrings()
    let fetchFeeds: () -> Void
    let onClose: () -> Void

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

                Button {
                    onClose()
                    appState.navigate(route: CommonViewRoute.accounts)
                } label: {
                    HStack {
                        Label {
                            Text(feedFlowStrings.settingsAccounts)
                                .foregroundStyle(Color.colorOnBackground)
                        } icon: {
                            Image(systemName: "arrow.triangle.2.circlepath")
                                .foregroundStyle(Color.accentColor)
                        }

                        Spacer()

                        Image(systemName: "chevron.right")
                            .font(.system(size: 14, weight: .semibold))
                            .foregroundStyle(Color(UIColor.tertiaryLabel))
                    }
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
