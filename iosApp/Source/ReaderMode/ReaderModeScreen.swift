import FeedFlowKit
import Foundation
import Reader
import SwiftUI

struct ReaderModeScreen: View {
    @Environment(BrowserSelector.self)
    private var browserSelector
    
    @Environment(\.openURL)
    private var openURL
    
    @Environment(AppState.self)
    private var appState

    @State private var showFontSizeMenu = false
    @State private var fontSize = 16.0
    @State private var isSliderMoving = false
    @State private var reset = false
    @State private var isBookmarked = false

    @StateObject private var vmStoreOwner = VMStoreOwner<ReaderModeViewModel>(
        Deps.shared.getReaderModeViewModel())

    let feedItemUrlInfo: FeedItemUrlInfo

    var body: some View {
        if let url = URL(string: feedItemUrlInfo.url) {
        ReaderView(
            url: url,
            options: ReaderViewOptions(
                additionalCSS: """
                    #__reader_container {
                        font-size: \(fontSize)px
                    }
                """,
                onLinkClicked: { url in
                    if browserSelector.openInAppBrowser() {
                        appState.navigate(route: CommonViewRoute.inAppBrowser(url: url))
                    } else {
                        openURL(
                            browserSelector.getUrlForDefaultBrowser(
                                stringUrl: url.absoluteString))
                    }
                }
            ),
            actions: ReaderViewActions(
                onBookmarkToggle: { newBookmarkState in
                    isBookmarked = newBookmarkState
                    vmStoreOwner.instance.updateBookmarkStatus(
                        feedItemId: FeedItemId(id: feedItemUrlInfo.id),
                        bookmarked: isBookmarked
                    )
                },
                onArchive: {
                    let archiveUrlString = getArchiveISUrl(articleUrl: feedItemUrlInfo.url)
                    if browserSelector.openInAppBrowser() {
                        if let archiveUrl = URL(string: archiveUrlString) {
                            appState.navigate(
                                route: CommonViewRoute.inAppBrowser(url: archiveUrl)
                            )
                        }
                    } else {
                        if let archiveUrl = URL(string: archiveUrlString) {
                            openURL(
                                browserSelector.getUrlForDefaultBrowser(
                                    stringUrl: archiveUrl.absoluteString))
                        }
                    }
                },
                onOpenInBrowser: {
                    if browserSelector.openInAppBrowser() {
                        if let urlForBrowser = URL(string: feedItemUrlInfo.url) {
                            appState.navigate(
                                route: CommonViewRoute.inAppBrowser(url: urlForBrowser)
                            )
                        }
                    } else {
                        if let urlForDefault = URL(string: feedItemUrlInfo.url) {
                            openURL(
                                browserSelector.getUrlForDefaultBrowser(
                                    stringUrl: urlForDefault.absoluteString))
                        }
                    }
                },
                onComments: feedItemUrlInfo.commentsUrl != nil ? {
                    if let commentsUrlString = feedItemUrlInfo.commentsUrl {
                        if browserSelector.openInAppBrowser() {
                            if let commentsUrl = URL(string: commentsUrlString) {
                                appState.navigate(
                                    route: CommonViewRoute.inAppBrowser(url: commentsUrl)
                                )
                            }
                        } else {
                            if let commentsUrl = URL(string: commentsUrlString) {
                                openURL(
                                    browserSelector.getUrlForDefaultBrowser(
                                        stringUrl: commentsUrl.absoluteString))
                            }
                        }
                    }
                } : nil,
                onFontSizeMenuToggle: {
                    showFontSizeMenu.toggle()
                },
                onFontSizeDecrease: {
                    fontSize -= 1.0
                    vmStoreOwner.instance.updateFontSize(newFontSize: Int32(Int(fontSize)))
                },
                onFontSizeIncrease: {
                    fontSize += 1.0
                    vmStoreOwner.instance.updateFontSize(newFontSize: Int32(Int(fontSize)))
                },
                onFontSizeChange: { newSize in
                    fontSize = newSize
                    vmStoreOwner.instance.updateFontSize(newFontSize: Int32(Int(fontSize)))
                }
            ),
            isBookmarked: isBookmarked,
            fontSize: fontSize,
            showFontSizeMenu: showFontSizeMenu
        )
        .onAppear {
            isBookmarked = feedItemUrlInfo.isBookmarked
        }
        .if(isiOS26OrLater()) { view in
            view.ignoresSafeArea()
        }
        .id(reset)
        .task {
            for await state in vmStoreOwner.instance.readerFontSizeState {
                self.fontSize = Double(truncating: state)
                self.reset.toggle()
            }
        }
        } else {
            Text("Invalid URL")
                .foregroundColor(.red)
        }
    }
}
