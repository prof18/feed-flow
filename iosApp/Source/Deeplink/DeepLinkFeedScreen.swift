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

    @State private var readerModeInfo: FeedItemUrlInfo?

    @State var feedId: String

    var body: some View {
        VStack {
            if state is DeeplinkFeedState.Error {
                Text(feedFlowStrings.genericErrorMessage)
            } else if let readerModeInfo = readerModeInfo {
                ReaderModeScreen(feedItemUrlInfo: readerModeInfo)
            } else {
                ProgressView()
            }
        }
        .onAppear {
            vmStoreOwner.instance.getReaderModeUrl(feedItemId: FeedItemId(id: feedId))
        }
        .task {
            for await state in vmStoreOwner.instance.deeplinkFeedState {
                self.state = state
                if let urlInfo = (state as? DeeplinkFeedState.Success)?.data {
                    switch urlInfo.linkOpeningPreference {
                    case .readerMode:
                        self.readerModeInfo = urlInfo
                    case .internalBrowser:
                        if let url = URL(string: urlInfo.url) {
                            appState.navigate(route: CommonViewRoute.inAppBrowser(url: url))
                        }
                    case .preferredBrowser:
                        openURL(browserSelector.getUrlForDefaultBrowser(stringUrl: urlInfo.url))
                        self.dismiss()
                    case .default:
                        if browserSelector.openReaderMode(link: urlInfo.url) {
                            self.readerModeInfo = urlInfo
                        } else if browserSelector.openInAppBrowser() {
                            if let url = URL(string: urlInfo.url) {
                                appState.navigate(route: CommonViewRoute.inAppBrowser(url: url))
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
}
