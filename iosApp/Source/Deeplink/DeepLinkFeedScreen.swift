import FeedFlowKit
import SwiftUI

struct DeepLinkFeedScreen: View {
    @Environment(\.openURL)
    private var openURL
    @Environment(BrowserSelector.self)
    private var browserSelector
    @Environment(\.dismiss)
    private var dismiss
    @Environment(AppState.self)
    private var appState

    @StateObject private var vmStoreOwner = VMStoreOwner<DeeplinkFeedViewModel>(
        Deps.shared.getDeeplinkFeedViewModel())

    @State private var state: DeeplinkFeedState = .Loading()
    @State private var shouldShowReaderMode = false

    let feedId: String
    let readerModeViewModel: ReaderModeViewModel

    var body: some View {
        Group {
            if state is DeeplinkFeedState.Error {
                ContentUnavailableView {
                    Label(feedFlowStrings.genericErrorMessage, systemImage: "exclamationmark.triangle")
                } actions: {
                    Button(feedFlowStrings.retryButton) {
                        loadFeed()
                    }
                }
            } else if shouldShowReaderMode {
                ReaderModeScreen(viewModel: readerModeViewModel, onInAppBrowserClick: nil)
                    .id(feedId)
            } else {
                ProgressView()
            }
        }
        .task(id: feedId) {
            loadFeed()
        }
        .task {
            for await state in vmStoreOwner.instance.deeplinkFeedState {
                self.state = state
                if state is DeeplinkFeedState.Error {
                    shouldShowReaderMode = false
                }
                if let urlInfo = (state as? DeeplinkFeedState.Success)?.data {
                    // URL-less items can only be shown in the reader from their feed content.
                    if urlInfo.url.isEmpty {
                        readerModeViewModel.getReaderModeHtml(urlInfo: urlInfo)
                        shouldShowReaderMode = true
                        vmStoreOwner.instance.markAsRead(feedItemId: FeedItemId(id: feedId))
                        continue
                    }
                    switch browserSelector.resolvedOpenMode(for: urlInfo) {
                    case .fullArticle, .feedContent:
                        readerModeViewModel.getReaderModeHtml(urlInfo: urlInfo)
                        shouldShowReaderMode = true
                    case .internalBrowser:
                        openInAppBrowser(urlString: urlInfo.url)
                    default:
                        if browserSelector.openInAppBrowser() {
                            openInAppBrowser(urlString: urlInfo.url)
                        } else {
                            openURL(browserSelector.getUrlForDefaultBrowser(stringUrl: urlInfo.url))
                            self.dismiss()
                        }
                    }
                    vmStoreOwner.instance.markAsRead(feedItemId: FeedItemId(id: feedId))
                }
            }
        }
    }

    private func loadFeed() {
        state = .Loading()
        shouldShowReaderMode = false
        readerModeViewModel.resetState()
        vmStoreOwner.instance.getReaderModeUrl(feedItemId: FeedItemId(id: feedId))
    }

    private func openInAppBrowser(urlString: String) {
        guard let url = URL(string: urlString) else {
            state = .Error()
            return
        }

        if browserSelector.isValidForInAppBrowser(url) {
            appState.openInAppBrowser(url: url)
            dismiss()
        } else {
            openURL(browserSelector.getUrlForDefaultBrowser(stringUrl: urlString))
            dismiss()
        }
    }
}
