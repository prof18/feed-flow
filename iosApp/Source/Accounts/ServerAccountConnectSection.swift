import SwiftUI

struct ServerAccountConnectSection: View {
    @State private var showConfirmation = false

    let isLoginLoading: Bool
    let hasLocalSubscriptions: Bool
    let isEnabled: Bool
    let onConnect: () -> Void

    var body: some View {
        Section {
            if hasLocalSubscriptions {
                Text(feedFlowStrings.serverAccountSubscriptionsWarning)
                    .font(.footnote)
                    .foregroundStyle(.secondary)
            }

            Button(
                action: {
                    if hasLocalSubscriptions {
                        showConfirmation = true
                    } else {
                        onConnect()
                    }
                },
                label: {
                    HStack {
                        Spacer()
                        Text(feedFlowStrings.accountConnectButton)
                        Spacer()
                    }
                }
            )
            .disabled(isLoginLoading || !isEnabled)
            .accessibilityIdentifier(AccountAccessibilityIdentifiers.connectButton)
        }
        .alert(feedFlowStrings.serverAccountReplaceLocalTitle, isPresented: $showConfirmation) {
            Button(feedFlowStrings.cancelButton, role: .cancel) {}
            Button(feedFlowStrings.serverAccountReplaceAndConnectButton, role: .destructive) {
                onConnect()
            }
        } message: {
            Text(feedFlowStrings.serverAccountReplaceLocalMessage)
        }
    }
}
