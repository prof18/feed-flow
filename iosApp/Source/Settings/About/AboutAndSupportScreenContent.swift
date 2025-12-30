import FeedFlowKit
import SwiftUI

struct AboutAndSupportScreenContent: View {
    @Binding var isCrashReportingEnabled: Bool
    @Environment(\.dismiss) private var dismiss

    let openURL: OpenURLAction
    let appState: AppState

    private let feedFlowStrings = Deps.shared.getStrings()

    var body: some View {
        Form {
            Section {
                Button(
                    action: {
                        let subject = feedFlowStrings.issueContentTitle
                        let content = feedFlowStrings.issueContentTemplate

                        if let url = URL(
                            string: Deps.shared.getUserFeedbackReporter().getEmailUrl(
                                subject: subject, content: content
                            )
                        ) {
                            openURL(url)
                        }
                    },
                    label: {
                        Label(feedFlowStrings.reportIssueButton, systemImage: "ladybug")
                    }
                )

                SettingToggleItem(
                    isOn: $isCrashReportingEnabled,
                    title: feedFlowStrings.settingsCrashReporting,
                    systemImage: "exclamationmark.bubble.fill"
                )

                if FeatureFlags.shared.ENABLE_FAQ {
                    Button {
                        let languageCode = Locale.current.language.languageCode?.identifier ?? "en"
                        let faqUrl = "https://feedflow.dev/\(languageCode)/faq"

                        if let url = URL(string: faqUrl) {
                            dismiss()
                            appState.navigate(route: CommonViewRoute.inAppBrowser(url: url))
                        }
                    } label: {
                        Label(feedFlowStrings.aboutMenuFaq, systemImage: "questionmark")
                    }
                }

                NavigationLink(destination: AboutScreen()) {
                    Label(feedFlowStrings.aboutButton, systemImage: "info.circle")
                }
            }
        }
        .scrollContentBackground(.hidden)
        .background(Color.secondaryBackgroundColor)
    }
}

#Preview {
    @Previewable @State var isCrashReportingEnabled = true
    @Previewable @State var appState = AppState()

    NavigationStack {
        AboutAndSupportScreenContent(
            isCrashReportingEnabled: $isCrashReportingEnabled,
            openURL: OpenURLAction { _ in .systemAction },
            appState: appState
        )
        .navigationTitle(Text("About & Support"))
        .navigationBarTitleDisplayMode(.inline)
    }
}
