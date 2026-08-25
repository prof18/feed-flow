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
    @State private var lineHeight = 0.0
    @State private var isBookmarked = false
    @State private var readerStatus = ReaderStatus.fetching
    @State private var currentContent: String?
    @State private var currentBaseUrl: String?
    @State private var articleUrl: URL?
    @State private var feedItemId: String?
    @State private var feedItemTitle: String?
    @State private var commentsUrl: String?
    @State private var currentImageUrl: String?
    @State private var currentSiteName: String?
    @State private var imageViewerUrl: URL?
    @State private var canNavigatePrevious = false
    @State private var canNavigateNext = false
    @State private var isShowingFeedContent = false
    @State private var hasArticleUrl = true
    @State private var canToggleContentSource = false
    // Mirrors the environment color scheme: `updateReaderHTML` runs inside a long-lived `.task`,
    // which captures the view value and with it a stale `@Environment` copy. `@State` is read
    // through its storage, so it stays current there.
    @State private var isDarkMode = false

    let viewModel: ReaderModeViewModel
    let onInAppBrowserClick: ((URL) -> Void)?

    var body: some View {
        ReaderView(
            readerStatus: $readerStatus,
            options: ReaderViewOptions(
                onLinkClicked: { url in
                    if browserSelector.openInAppBrowser() {
                        if let browserClick = onInAppBrowserClick {
                            browserClick(url)
                        } else {
                            appState.openInAppBrowser(url: url)
                        }
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
                    lineHeight: feedFlowStrings.readerModeLineHeight,
                    textSettings: feedFlowStrings.readerModeTextSettings,
                    resetToDefault: feedFlowStrings.readerModeResetToDefault,
                    done: feedFlowStrings.actionDone,
                    previousArticle: feedFlowStrings.previousArticle,
                    nextArticle: feedFlowStrings.nextArticle,
                    feedContent: feedFlowStrings.readerContentSourceFeed,
                    contentUnavailableTitle: feedFlowStrings.readerModeNoContentTitle,
                    contentUnavailableMessage: feedFlowStrings.readerModeNoContentMessage
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
                                if let browserClick = onInAppBrowserClick {
                                    browserClick(archiveUrl)
                                } else {
                                    appState.navigate(
                                        route: CommonViewRoute.inAppBrowser(url: archiveUrl)
                                    )
                                }
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
                    if let commentsUrlString = commentsUrl,
                       let commUrl = URL(string: commentsUrlString) {
                        if browserSelector.openInAppBrowser() {
                            if let browserClick = onInAppBrowserClick {
                                browserClick(commUrl)
                            } else {
                                appState.navigate(
                                    route: CommonViewRoute.inAppBrowser(url: commUrl)
                                )
                            }
                        } else {
                            openURL(
                                browserSelector.getUrlForDefaultBrowser(
                                    stringUrl: commUrl.absoluteString))
                        }
                    }
                } : nil,
                onFontSizeMenuToggle: {
                    showFontSizeMenu.toggle()
                },
                onFontSizeChange: { newSize in
                    fontSize = newSize
                    viewModel.updateFontSize(newFontSize: Int32(Int(fontSize)))
                },
                onLineHeightChange: { newValue in
                    lineHeight = newValue
                    viewModel.updateLineHeight(newLineHeight: Int32(Int(newValue)))
                },
                onNavigateToNext: canNavigateNext ? {
                    viewModel.navigateToNextArticle()
                } : nil,
                onNavigateToPrevious: canNavigatePrevious ? {
                    viewModel.navigateToPreviousArticle()
                } : nil,
                onToggleContentSource: canToggleContentSource ? {
                    viewModel.toggleContentSource()
                } : nil,
                isShowingFeedContent: isShowingFeedContent,
                hasUrl: hasArticleUrl
            ),
            isBookmarked: isBookmarked,
            fontSize: fontSize,
            lineHeight: lineHeight,
            defaultFontSize: Double(ReaderModeDefaults.shared.FONT_SIZE),
            defaultLineHeight: Double(ReaderModeDefaults.shared.LINE_HEIGHT),
            showFontSizeMenu: $showFontSizeMenu,
            openInBrowser: { url in
                openInBrowser(url: url)
            }
        )
        .ignoresSafeArea(edges: isiOS26OrLater() ? .all : [])
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
        .onChange(of: colorScheme, initial: true) { _, newValue in
            isDarkMode = newValue == .dark
        }
        .task {
            for await settings in viewModel.readerFontSettingsState {
                self.fontSize = Double(settings.fontSize)
                self.lineHeight = Double(settings.lineHeight)
            }
        }
        .task {
            for await state in viewModel.readerModeState {
                switch onEnum(of: state) {
                case let .htmlNotAvailable(data):
                    self.feedItemId = data.id
                    self.hasArticleUrl = !data.url.isEmpty
                    self.canToggleContentSource = false
                    self.currentSiteName = nil
                    // A url-less item has no page to fall back to, so there is nothing to load.
                    if let url = URL(string: data.url) {
                        self.articleUrl = url
                        self.readerStatus = .failedToExtractContent(url: url)
                    } else {
                        self.readerStatus = .contentUnavailable
                    }
                case .loading:
                    self.readerStatus = .fetching
                    self.canToggleContentSource = false
                    self.currentSiteName = nil
                case let .success(data):
                    let readerModeData = data.readerModeData

                    self.feedItemId = readerModeData.id.id
                    self.feedItemTitle = readerModeData.title
                    self.commentsUrl = readerModeData.commentsUrl
                    self.currentContent = readerModeData.content
                    self.currentBaseUrl = readerModeData.baseUrl
                    self.currentImageUrl = readerModeData.imageUrl
                    self.currentSiteName = readerModeData.siteName
                    self.isShowingFeedContent = readerModeData.shownContentSource == .feed
                    self.hasArticleUrl = !readerModeData.url.isEmpty
                    self.canToggleContentSource = readerModeData.canToggleContentSource
                    let url = URL(string: readerModeData.url) ?? URL(fileURLWithPath: "")
                    self.articleUrl = url

                    updateReaderHTML()
                }

                self.isBookmarked = state.getIsBookmarked
            }
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
            if let browserClick = onInAppBrowserClick {
                browserClick(url)
            } else {
                appState.openInAppBrowser(url: url)
            }
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
            lineHeight: Int32(lineHeight),
            title: isShowingFeedContent ? feedItemTitle : nil,
            imageUrl: currentImageUrl,
            leadingContent: "",
            siteName: isShowingFeedContent ? currentSiteName : nil
        )

        self.readerStatus = .extractedContent(
            html: html,
            baseURL: URL(string: baseUrlString) ?? URL(fileURLWithPath: ""),
            url: url,
            contentId: currentContentId(content: content)
        )
    }

    /// Identifies the document the reader is showing, ignoring everything that only styles it.
    /// The web view reloads when this changes, so font size, line height and theme must stay out
    /// of it or the scroll position is lost every time one of them is applied.
    private func currentContentId(content: String) -> String {
        var hasher = Hasher()
        hasher.combine(feedItemId)
        hasher.combine(isShowingFeedContent)
        hasher.combine(content)
        hasher.combine(feedItemTitle)
        hasher.combine(currentSiteName)
        hasher.combine(currentImageUrl)
        return String(hasher.finalize())
    }

    private var themeColors: ReaderThemeColors {
        ReaderThemeColors(
            textColor: isDarkMode ? "#FFFFFF" : "#000000",
            linkColor: isDarkMode ? "#3B82F6" : "#2563EB",
            backgroundColor: isDarkMode ? "#1e1e1e" : "#f6f8fa",
            borderColor: isDarkMode ? "#444444" : "#d1d9e0"
        )
    }
}
