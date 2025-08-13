import FeedFlowKit
import Foundation
import Reader
import SwiftUI

struct ReaderModeScreen: View {
    @Environment(BrowserSelector.self) private var browserSelector
    @Environment(\.openURL) private var openURL
    @Environment(AppState.self) private var appState

    @State private var showFontSizeMenu: Bool = false
    @State private var fontSize = 16.0
    @State private var isSliderMoving = false
    @State private var reset = false
    @State private var isBookmarked = false

    @StateObject private var vmStoreOwner = VMStoreOwner<ReaderModeViewModel>(
        Deps.shared.getReaderModeViewModel())

    let feedItemUrlInfo: FeedItemUrlInfo

    var body: some View {
        ReaderView(
            url: URL(string: feedItemUrlInfo.url)!,
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
                        appState.navigate(
                            route: CommonViewRoute.inAppBrowser(url: URL(string: archiveUrlString)!)
                        )
                    } else {
                        openURL(
                            browserSelector.getUrlForDefaultBrowser(
                                stringUrl: URL(string: archiveUrlString)!.absoluteString))
                    }
                },
                onOpenInBrowser: {
                    if browserSelector.openInAppBrowser() {
                        appState.navigate(
                            route: CommonViewRoute.inAppBrowser(url: URL(string: feedItemUrlInfo.url)!)
                        )
                    } else {
                        openURL(
                            browserSelector.getUrlForDefaultBrowser(
                                stringUrl: URL(string: feedItemUrlInfo.url)!.absoluteString))
                    }
                },
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
        .ignoresSafeArea()
        .id(reset)
        .task {
            for await state in vmStoreOwner.instance.readerFontSizeState {
                self.fontSize = Double(truncating: state)
                self.reset.toggle()
            }
        }
    }
}
