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

    @State var feedId: String
    let readerModeViewModel: ReaderModeViewModel

    var body: some View {
        ReaderModeScreen(viewModel: readerModeViewModel)
        .onAppear {
            vmStoreOwner.instance.getReaderModeUrl(feedItemId: FeedItemId(id: feedId))
        }
        .task {
            for await state in vmStoreOwner.instance.deeplinkFeedState {
                self.state = state
                if let urlInfo = (state as? DeeplinkFeedState.Success)?.data {
                    switch urlInfo.linkOpeningPreference {
                    case .readerMode:
                        readerModeViewModel.getReaderModeHtml(urlInfo: urlInfo)
                    case .internalBrowser:
                        if let url = URL(string: urlInfo.url) {
                            if browserSelector.isValidForInAppBrowser(url) {
                                appState.navigate(route: CommonViewRoute.inAppBrowser(url: url))
                            } else {
                                openURL(browserSelector.getUrlForDefaultBrowser(stringUrl: urlInfo.url))
                                self.dismiss()
                            }
                        }
                    case .preferredBrowser:
                        openURL(browserSelector.getUrlForDefaultBrowser(stringUrl: urlInfo.url))
                        self.dismiss()
                    case .default:
                        if browserSelector.openReaderMode(link: urlInfo.url) {
                            readerModeViewModel.getReaderModeHtml(urlInfo: urlInfo)
                        } else if browserSelector.openInAppBrowser() {
                            if let url = URL(string: urlInfo.url) {
                                if browserSelector.isValidForInAppBrowser(url) {
                                    appState.navigate(route: CommonViewRoute.inAppBrowser(url: url))
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
}
