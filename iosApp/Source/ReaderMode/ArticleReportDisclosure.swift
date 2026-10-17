import FeedFlowKit
import SwiftUI

struct ArticleReportDisclosure: ViewModifier {
    let state: ArticleReportUiState?
    let onSend: () -> Void
    let onCancel: () -> Void
    let onDismiss: () -> Void

    func body(content: Content) -> some View {
        content
            .alert(feedFlowStrings.articleReportAction, isPresented: Binding(
                get: { state?.showDisclosure == true },
                set: {
                    if !$0 {
                        onCancel()
                    }
                }
            )) {
                Button(feedFlowStrings.articleReportSend, action: onSend)
                    .accessibilityIdentifier(ReaderReportAccessibilityIdentifiers.send)
                Button(feedFlowStrings.cancelButton, role: .cancel, action: onCancel)
                    .accessibilityIdentifier(ReaderReportAccessibilityIdentifiers.cancel)
            } message: {
                Text(feedFlowStrings.articleReportDisclosure)
            }
            .alert(message, isPresented: Binding(
                get: { state?.message != nil && state?.message != .sending && state?.showDisclosure == false },
                set: {
                    if !$0 {
                        onDismiss()
                    }
                }
            )) {
                Button(feedFlowStrings.actionDone, role: .cancel, action: onDismiss)
            }
    }

    private var message: String {
        guard let message = state?.message else { return "" }
        switch message {
        case .sending: return feedFlowStrings.articleReportSending
        case .sent: return feedFlowStrings.articleReportSent
        case .failed: return feedFlowStrings.articleReportFailed
        }
    }
}
