import Foundation
import SwiftUI

extension View {
    func websiteVerificationExplanation(
        url: String?,
        identity: String? = nil,
        readerHint: Bool = false,
        onCancel: @escaping () -> Void = {},
        onConfirm: @escaping () -> Void
    ) -> some View {
        modifier(
            WebsiteVerificationExplanationModifier(
                url: url,
                identity: identity,
                readerHint: readerHint,
                onCancel: onCancel,
                onConfirm: onConfirm
            )
        )
    }
}

private struct WebsiteVerificationExplanationModifier: ViewModifier {
    let url: String?
    let identity: String?
    let readerHint: Bool
    let onCancel: () -> Void
    let onConfirm: () -> Void

    @State private var activeExplanation: WebsiteVerificationExplanationItem?
    @State private var lastPresentedIdentity: String?

    private var explanation: WebsiteVerificationExplanationItem? {
        url.map { WebsiteVerificationExplanationItem(url: $0, id: identity ?? $0) }
    }

    func body(content: Content) -> some View {
        content
            .onChange(of: explanation, initial: true) { _, newExplanation in
                guard let newExplanation else {
                    lastPresentedIdentity = nil
                    activeExplanation = nil
                    return
                }
                guard lastPresentedIdentity != newExplanation.id else { return }
                lastPresentedIdentity = newExplanation.id
                activeExplanation = newExplanation
            }
            .alert(item: $activeExplanation) { item in
                let host = URL(string: item.url)?.host ?? item.url
                let readerOptions = readerHint
                    ? "\n\n\(feedFlowStrings.readerVerificationContentOptions)"
                    : ""
                let message = "\(host)\n\n\(feedFlowStrings.feedVerificationExplanationBody)\(readerOptions)"

                return Alert(
                    title: Text(feedFlowStrings.feedVerificationExplanationTitle),
                    message: Text(message),
                    primaryButton: .default(Text(feedFlowStrings.feedVerificationOpenWebsite), action: onConfirm),
                    secondaryButton: .cancel(Text(feedFlowStrings.cancelButton), action: onCancel)
                )
            }
    }
}
