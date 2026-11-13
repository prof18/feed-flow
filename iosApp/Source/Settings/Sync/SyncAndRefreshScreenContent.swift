import FeedFlowKit
import SwiftUI

struct SyncAndRefreshScreenContent: View {
    @Binding var refreshFeedsOnLaunch: Bool

    private let feedFlowStrings = Deps.shared.getStrings()

    var body: some View {
        Form {
            Section {
                NavigationLink(destination: AccountsScreen()) {
                    Label(feedFlowStrings.settingsAccounts, systemImage: "person.crop.circle")
                }
                .accessibilityIdentifier(SettingsAccessibilityIdentifiers.accountsRow)
            }

            Section {
                Toggle(isOn: $refreshFeedsOnLaunch) {
                    Text(feedFlowStrings.settingsRefreshFeedsOnLaunch)
                }
                .accessibilityIdentifier(SyncAndRefreshAccessibilityIdentifiers.refreshOnLaunch)
            } header: {
                Text(feedFlowStrings.settingsRefreshSectionTitle)
            }
        }
        .scrollContentBackground(.hidden)
        .background(Color.secondaryBackgroundColor)
    }
}
