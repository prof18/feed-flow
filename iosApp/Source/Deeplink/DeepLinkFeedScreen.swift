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
                    switch urlInfo.linkOpeningPreference {
                    case .readerMode:
                        if browserSelector.isReaderModeEligible(link: urlInfo.url) {
                            readerModeViewModel.getReaderModeHtml(urlInfo: urlInfo)
                            shouldShowReaderMode = true
                        } else {
                            openURL(browserSelector.getUrlForDefaultBrowser(stringUrl: urlInfo.url))
                            self.dismiss()
                        }
                    case .internalBrowser:
                        if let url = URL(string: urlInfo.url) {
                            if browserSelector.isValidForInAppBrowser(url) {
                                appState.openInAppBrowser(url: url)
                            } else {
                                openURL(browserSelector.getUrlForDefaultBrowser(stringUrl: urlInfo.url))
                                self.dismiss()
                            }
                        }
                    case .preferredBrowser:
                        openURL(browserSelector.getUrlForDefaultBrowser(stringUrl: urlInfo.url))
                        self.dismiss()
                    case .default:
                        if browserSelector.shouldOpenInReaderMode(link: urlInfo.url) {
                            readerModeViewModel.getReaderModeHtml(urlInfo: urlInfo)
                            shouldShowReaderMode = true
                        } else if browserSelector.openInAppBrowser() {
                            if let url = URL(string: urlInfo.url) {
                                if browserSelector.isValidForInAppBrowser(url) {
                                    appState.openInAppBrowser(url: url)
                                } else {
                                    openURL(browserSelector.getUrlForDefaultBrowser(stringUrl: urlInfo.url))
                                    self.dismiss()
                                }
                            }
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
}
