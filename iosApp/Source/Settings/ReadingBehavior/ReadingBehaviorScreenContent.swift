import FeedFlowKit
import SwiftUI

struct ReadingBehaviorScreenContent: View {
    @Bindable var browserSelector: BrowserSelector
    @Binding var articleOpenMode: ArticleOpenMode
    let onArticleBrowserSelected: (String) -> Void
    @Binding var isMarkReadWhenScrollingEnabled: Bool
    @Binding var readArticlesVisibility: ReadArticlesVisibility

    private let feedFlowStrings = Deps.shared.getStrings()

    var body: some View {
        Form {
            Section {
                NavigationLink {
                    ArticleOpenModeSelectionScreen(
                        articleOpenMode: $articleOpenMode,
                        browsers: browserSelector.browsers,
                        selectedArticleBrowserId: selectedArticleBrowserId,
                        onBrowserSelected: onArticleBrowserSelected
                    )
                } label: {
                    LabeledContent(feedFlowStrings.articleOpenMode, value: articleOpenModePreview)
                }
                .accessibilityIdentifier(ReadingBehaviorAccessibilityIdentifiers.articleOpenModePicker)
            } footer: {
                Text(feedFlowStrings.articleOpenModeSubtitle)
            }

            Section {
                NavigationLink {
                    LinksBrowserSelectionScreen(browserSelector: browserSelector)
                } label: {
                    LabeledContent(
                        feedFlowStrings.linksBrowserTitle,
                        value: browserSelector.selectedBrowser?.name ?? feedFlowStrings.inAppBrowser
                    )
                }
                .accessibilityIdentifier(ReadingBehaviorAccessibilityIdentifiers.linksBrowserPicker)
            } footer: {
                Text(feedFlowStrings.linksBrowserSubtitle)
            }

            Section {
                Toggle(isOn: $isMarkReadWhenScrollingEnabled) {
                    Text(feedFlowStrings.toggleMarkReadWhenScrolling)
                }
                .accessibilityIdentifier(ReadingBehaviorAccessibilityIdentifiers.markReadWhenScrollingToggle)

                Picker(selection: $readArticlesVisibility) {
                    Text(feedFlowStrings.settingsReadArticlesKeep)
                        .tag(ReadArticlesVisibility.keepInList)
                    Text(feedFlowStrings.settingsReadArticlesHideOnRefresh)
                        .tag(ReadArticlesVisibility.hideOnRefresh)
                    Text(feedFlowStrings.settingsReadArticlesHideRightAway)
                        .tag(ReadArticlesVisibility.hideRightAway)
                        .selectionDisabled(isMarkReadWhenScrollingEnabled)
                        .accessibilityIdentifier(ReadingBehaviorAccessibilityIdentifiers.readArticlesHideRightAwayOption)
                } label: {
                    Text(feedFlowStrings.settingsReadArticlesTitle)
                }
                .pickerStyle(.inline)
            } footer: {
                if isMarkReadWhenScrollingEnabled {
                    Text(feedFlowStrings.settingsReadArticlesRightAwayDisabled)
                } else {
                    Text(feedFlowStrings.settingsReadArticlesFooter)
                }
            }
        }
        .scrollContentBackground(.hidden)
        .background(Color.secondaryBackgroundColor)
    }

    private var selectedArticleBrowserId: String? {
        switch articleOpenMode {
        case .internalBrowser:
            BrowserIds.shared.IN_APP_BROWSER
        case .preferredBrowser:
            browserSelector.articleBrowserId()
        default:
            nil
        }
    }

    private var articleOpenModePreview: String {
        switch articleOpenMode {
        case .fullArticle:
            feedFlowStrings.articleOpenModeFullArticleShort
        case .feedContent:
            feedFlowStrings.articleOpenModeFeedShort
        case .internalBrowser:
            feedFlowStrings.inAppBrowser
        default:
            browserSelector.articleBrowserId() == BrowserIds.shared.IN_APP_BROWSER
                ? feedFlowStrings.inAppBrowser
                : browserSelector.articleBrowserName()
        }
    }
}
