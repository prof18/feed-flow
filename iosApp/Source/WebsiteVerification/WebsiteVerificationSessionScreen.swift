import SwiftUI

struct WebsiteVerificationSessionScreen: View {
    @Environment(\.dismiss)
    private var dismiss
    @State private var session: WebsiteBrowserSession

    init(sessionId: String) {
        _session = State(initialValue: WebsiteBrowserSession(sessionId: sessionId))
    }

    var body: some View {
        NavigationStack {
            VStack(alignment: .leading, spacing: 20) {
                switch session.pageState {
                case .preparing, .checking:
                    VStack(spacing: 12) {
                        ProgressView()
                            .controlSize(.large)
                        Text(feedFlowStrings.feedVerificationCheckingTitle)
                            .font(.headline)
                        Text(feedFlowStrings.feedVerificationCheckingBody)
                            .multilineTextAlignment(.center)
                            .foregroundStyle(.secondary)
                    }
                    .frame(maxWidth: .infinity)
                case .interactive:
                    // No manual button here: a button suggests the user must tap it, while the
                    // screen continues on its own after navigation or the periodic re-check.
                    Text(feedFlowStrings.feedVerificationAutomaticHint)
                        .multilineTextAlignment(.leading)
                case .failed:
                    ContentUnavailableView(
                        feedFlowStrings.feedVerificationFailed,
                        systemImage: "exclamationmark.triangle",
                        description: Text(failureDescription)
                    )
                case .complete:
                    VStack(spacing: 12) {
                        ProgressView()
                            .controlSize(.large)
                        Text(feedFlowStrings.feedVerificationCheckingBody)
                            .multilineTextAlignment(.center)
                            .foregroundStyle(.secondary)
                    }
                    .frame(maxWidth: .infinity)
                }

                WebsiteVerificationWebView(webView: session.webView)
                    .id(ObjectIdentifier(session.webView))
                    .opacity(session.isBrowserVisible ? 1 : 0)
                    .frame(maxHeight: session.isBrowserVisible ? .infinity : 1)
                    .allowsHitTesting(session.isBrowserVisible)
                    .accessibilityHidden(!session.isBrowserVisible)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding()
            .navigationTitle(feedFlowStrings.feedVerificationScreenTitle)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(feedFlowStrings.cancelButton) {
                        session.close()
                        dismiss()
                    }
                    .accessibilityIdentifier(WebsiteVerificationAccessibilityIDs.cancelButton)
                }
                ToolbarItem(placement: .principal) {
                    Text(session.host)
                        .font(.subheadline)
                        .lineLimit(1)
                }
            }
            .task { session.start() }
            .task(id: session.pageState) {
                guard session.pageState == .interactive else { return }
                while !Task.isCancelled {
                    try? await Task.sleep(for: Self.recheckInterval)
                    guard !Task.isCancelled else { return }
                    session.recheckSilently()
                }
            }
            .onDisappear { session.close() }
        }
        .accessibilityIdentifier(WebsiteVerificationAccessibilityIDs.screen)
    }

    private static let recheckInterval: Duration = .seconds(4)

    private var failureDescription: String {
        switch session.failure {
        case .browserStopped: feedFlowStrings.feedVerificationBrowserFailed
        case .unsupported: feedFlowStrings.feedVerificationUnsupported
        default: feedFlowStrings.feedVerificationFailed
        }
    }
}
