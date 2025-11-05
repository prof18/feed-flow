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

    @Environment(\.colorScheme)
    private var colorScheme

    @State private var showFontSizeMenu = false
    @State private var fontSize = 16.0
    @State private var isBookmarked = false
    @State private var readerStatus = ReaderStatus.fetching
    @State private var currentContent: String?
    @State private var currentBaseUrl: String?
    @State private var articleUrl: URL?

    @StateObject private var vmStoreOwner = VMStoreOwner<ReaderModeViewModel>(
        Deps.shared.getReaderModeViewModel())

    let feedItemUrlInfo: FeedItemUrlInfo

    var body: some View {
        ReaderView(
            readerStatus: $readerStatus,
            options: ReaderViewOptions(
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
                    if let url = articleUrl {
                        openInBrowser(url: url)
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
            showFontSizeMenu: showFontSizeMenu,
            openInBrowser: { url in
                openInBrowser(url: url)
            }
        )
        .onAppear {
            isBookmarked = feedItemUrlInfo.isBookmarked
            vmStoreOwner.instance.getReaderModeHtml(urlInfo: feedItemUrlInfo)
        }
        .if(isiOS26OrLater()) { view in
            view.ignoresSafeArea()
        }
        .task {
            vmStoreOwner.instance.getReaderModeHtml(urlInfo: feedItemUrlInfo)
        }
        .task {
            for await state in vmStoreOwner.instance.readerFontSizeState {
                self.fontSize = Double(truncating: state)
            }
        }
        .task {
            for await state in vmStoreOwner.instance.readerModeState {
                switch onEnum(of: state) {
                case let .htmlNotAvailable(data):
                    let url = URL(string: data.url) ?? URL(fileURLWithPath: "")
                    self.articleUrl = url
                    self.readerStatus = .failedToExtractContent(url: url)
                case .loading:
                    self.readerStatus = .fetching
                case let .success(data):
                    let readerModeData = data.readerModeData

                    self.currentContent = readerModeData.content
                    self.currentBaseUrl = readerModeData.url
                    let url = URL(string: readerModeData.url) ?? URL(fileURLWithPath: "")
                    self.articleUrl = url

                    updateReaderHTML()
                }

                self.isBookmarked = state.getIsBookmarked
            }
        }
        .onChange(of: colorScheme) { _, _ in
            updateReaderHTML()
        }
    }

    private func openInBrowser(url: URL) {
        if browserSelector.openInAppBrowser() {
            appState.navigate(route: CommonViewRoute.inAppBrowser(url: url))
        } else {
            openURL(browserSelector.getUrlForDefaultBrowser(stringUrl: url.absoluteString))
        }
    }

    private func updateReaderHTML() {
        guard let content = currentContent,
              let baseUrlString = currentBaseUrl,
              let url = articleUrl else { return }

        let isDarkMode = colorScheme == .dark
        let html = getReaderModeStyledHtml(
            colors: ReaderColors(
                textColor: isDarkMode ? "#FFFFFF" : "#000000",
                linkColor: isDarkMode ? "#3B82F6" : "#2563EB",
                backgroundColor: isDarkMode ? "#1e1e1e" : "#f6f8fa",
                borderColor: isDarkMode ? "#444444" : "#d1d9e0"
            ),
            content: content,
            fontSize: Int32(fontSize),
            title: feedItemUrlInfo.title,
        )

        self.readerStatus = .extractedContent(
            html: html,
            baseURL: URL(string: baseUrlString) ?? URL(fileURLWithPath: ""),
            url: url
        )
    }
}
