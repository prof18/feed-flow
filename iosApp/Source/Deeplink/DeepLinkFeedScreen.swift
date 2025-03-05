import FeedFlowKit
import SwiftUI

struct DeepLinkFeedScreen: View {
    @Environment(\.openURL) private var openURL
    @Environment(BrowserSelector.self) private var browserSelector
    @Environment(\.dismiss) private var dismiss

    @StateObject
    private var vmStoreOwner = VMStoreOwner<DeeplinkFeedViewModel>(
        Deps.shared.getDeeplinkFeedViewModel())

    @State private var state: DeeplinkFeedState = .Loading()

    @State private var browserToOpen: BrowserToPresent?

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
                        browserToOpen = .inAppBrowser(url: URL(string: urlInfo.url)!)
                    case .preferredBrowser:
                        openURL(browserSelector.getUrlForDefaultBrowser(stringUrl: urlInfo.url))
                        self.dismiss()
                    case .default:
                        if browserSelector.openReaderMode(link: urlInfo.url) {
                            self.readerModeInfo = urlInfo
                        } else if browserSelector.openInAppBrowser() {
                            browserToOpen = .inAppBrowser(url: URL(string: urlInfo.url)!)
                        } else {
                            openURL(browserSelector.getUrlForDefaultBrowser(stringUrl: urlInfo.url))
                            self.dismiss()
                        }
                    }
                    vmStoreOwner.instance.markAsRead(feedItemId: FeedItemId(id: feedId))
                }
            }
        }
        .fullScreenCover(item: $browserToOpen) { browserToOpen in
            switch browserToOpen {
            case let .inAppBrowser(url):
                SFSafariView(url: url)
                    .ignoresSafeArea()
            }
        }
        .onChange(of: browserToOpen) { _, newValue in
            if newValue == nil {
                self.dismiss()
            }
        }
    }
}
