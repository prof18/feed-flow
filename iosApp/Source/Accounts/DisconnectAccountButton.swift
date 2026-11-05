import SwiftUI

struct DisconnectAccountButton: View {
    @State private var showConfirmation = false

    let kind: DisconnectAccountKind
    let isDisabled: Bool
    let onConfirm: () -> Void

    var body: some View {
        Button(
            role: .destructive,
            action: { showConfirmation = true },
            label: {
                // Form rows draw the icon in the accent colour even with a destructive role.
                Label {
                    Text(feedFlowStrings.accountDisconnectButton)
                } icon: {
                    Image(systemName: "square.slash")
                        .foregroundStyle(isDisabled ? Color.secondary : Color.red)
                }
            }
        )
        .disabled(isDisabled)
        .accessibilityIdentifier(AccountAccessibilityIdentifiers.disconnectButton)
        .onChange(of: isDisabled) { _, disabled in
            // A sync starting while the alert is open closes it.
            if disabled { showConfirmation = false }
        }
        .alert(feedFlowStrings.accountDisconnectTitle, isPresented: $showConfirmation) {
            Button(feedFlowStrings.cancelButton, role: .cancel) {}
            Button(feedFlowStrings.accountDisconnectButton, role: .destructive) {
                if !isDisabled { onConfirm() }
            }
            .accessibilityIdentifier(AccountAccessibilityIdentifiers.disconnectConfirmButton)
        } message: {
            Text(kind.message)
        }
    }
}
