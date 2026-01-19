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
    @State private var feedItemId: String?
    @State private var feedItemTitle: String?
    @State private var commentsUrl: String?
    @State private var imageViewerUrl: URL?
    @State private var canNavigatePrevious = false
    @State private var canNavigateNext = false
    @State private var macOSThemeChangeToken = UUID()

    let viewModel: ReaderModeViewModel

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
                },
                onImageClicked: { url in
                    imageViewerUrl = url
                }
            ),
            themeColors: themeColors,
            actions: ReaderViewActions(
                strings: ReaderViewStrings(
                    share: feedFlowStrings.menuShare,
                    addBookmark: feedFlowStrings.menuAddToBookmark,
                    removeBookmark: feedFlowStrings.menuRemoveFromBookmark,
                    openInArchive: feedFlowStrings.readerModeArchiveButton,
                    openComments: feedFlowStrings.menuOpenComments,
                    fontSize: feedFlowStrings.readerModeFontSize,
                    previousArticle: feedFlowStrings.previousArticle,
                    nextArticle: feedFlowStrings.nextArticle
                ),
                onBookmarkToggle: { newBookmarkState in
                    if let id = feedItemId {
                        isBookmarked = newBookmarkState
                        viewModel.updateBookmarkStatus(
                            feedItemId: FeedItemId(id: id),
                            bookmarked: isBookmarked
                        )
                    }
                },
                onArchive: {
                    if let url = articleUrl {
                        let archiveUrlString = getArchiveISUrl(articleUrl: url.absoluteString)
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
                    }
                },
                onOpenInBrowser: {
                    if let url = articleUrl {
                        openInBrowser(url: url)
                    }
                },
                onComments: commentsUrl != nil ? {
                    if let commentsUrlString = commentsUrl {
                        if browserSelector.openInAppBrowser() {
                            if let commUrl = URL(string: commentsUrlString) {
                                appState.navigate(
                                    route: CommonViewRoute.inAppBrowser(url: commUrl)
                                )
                            }
                        } else {
                            if let commUrl = URL(string: commentsUrlString) {
                                openURL(
                                    browserSelector.getUrlForDefaultBrowser(
                                        stringUrl: commUrl.absoluteString))
                            }
                        }
                    }
                } : nil,
                onFontSizeMenuToggle: {
                    showFontSizeMenu.toggle()
                },
                onFontSizeDecrease: {
                    fontSize -= 1.0
                    viewModel.updateFontSize(newFontSize: Int32(Int(fontSize)))
                },
                onFontSizeIncrease: {
                    fontSize += 1.0
                    viewModel.updateFontSize(newFontSize: Int32(Int(fontSize)))
                },
                onFontSizeChange: { newSize in
                    fontSize = newSize
                    viewModel.updateFontSize(newFontSize: Int32(Int(fontSize)))
                },
                onNavigateToNext: canNavigateNext ? {
                    viewModel.navigateToNextArticle()
                } : nil,
                onNavigateToPrevious: canNavigatePrevious ? {
                    viewModel.navigateToPreviousArticle()
                } : nil
            ),
            isBookmarked: isBookmarked,
            fontSize: fontSize,
            showFontSizeMenu: $showFontSizeMenu,
            openInBrowser: { url in
                openInBrowser(url: url)
            }
        )
        .if(isiOS26OrLater()) { view in
            view.ignoresSafeArea()
        }
        .fullScreenCover(
            isPresented: Binding(
                get: { imageViewerUrl != nil },
                set: { if !$0 { imageViewerUrl = nil } }
            )
        ) {
            if let imageUrl = imageViewerUrl {
                ReaderImageViewer(
                    imageUrl: imageUrl,
                    onClose: { imageViewerUrl = nil }
                )
            }
        }
        .task {
            for await state in viewModel.readerFontSizeState {
                self.fontSize = Double(truncating: state)
            }
        }
        .task {
            for await state in viewModel.readerModeState {
                switch onEnum(of: state) {
                case let .htmlNotAvailable(data):
                    self.feedItemId = data.id
                    let url = URL(string: data.url) ?? URL(fileURLWithPath: "")
                    self.articleUrl = url
                    self.readerStatus = .failedToExtractContent(url: url)
                case .loading:
                    self.readerStatus = .fetching
                case let .success(data):
                    let readerModeData = data.readerModeData

                    self.feedItemId = readerModeData.id.id
                    self.feedItemTitle = readerModeData.title
                    self.commentsUrl = readerModeData.commentsUrl
                    self.currentContent = readerModeData.content
                    self.currentBaseUrl = readerModeData.baseUrl
                    let url = URL(string: readerModeData.url) ?? URL(fileURLWithPath: "")
                    self.articleUrl = url

                    updateReaderHTML()
                }

                self.isBookmarked = state.getIsBookmarked
            }
        }
        .onChange(of: themeColors) { _, _ in
            updateReaderHTML()
        }
        .task {
            for await canNavigate in viewModel.canNavigateToPreviousState {
                self.canNavigatePrevious = canNavigate.boolValue
            }
        }
        .task {
            for await canNavigate in viewModel.canNavigateToNextState {
                self.canNavigateNext = canNavigate.boolValue
            }
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

        let colors = themeColors
        let html = getReaderModeStyledHtml(
            colors: ReaderColors(
                textColor: colors.textColor,
                linkColor: colors.linkColor,
                backgroundColor: colors.backgroundColor,
                borderColor: colors.borderColor
            ),
            content: content,
            fontSize: Int32(fontSize),
            title: nil
        )

        self.readerStatus = .extractedContent(
            html: html,
            baseURL: URL(string: baseUrlString) ?? URL(fileURLWithPath: ""),
            url: url
        )
    }

    private var themeColors: ReaderThemeColors {
        let isDarkMode = currentIsDarkMode
        return ReaderThemeColors(
            textColor: isDarkMode ? "#FFFFFF" : "#000000",
            linkColor: isDarkMode ? "#3B82F6" : "#2563EB",
            backgroundColor: isDarkMode ? "#1e1e1e" : "#f6f8fa",
            borderColor: isDarkMode ? "#444444" : "#d1d9e0"
        )
    }

    private var currentIsDarkMode: Bool {
        return colorScheme == .dark
    }
}
