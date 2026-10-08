import FeedFlowKit
import Foundation
import Reader
import SwiftUI
import UIKit

struct ReaderModeScreen: View {
    @Environment(BrowserSelector.self)
    private var browserSelector

    @Environment(\.openURL)
    private var openURL

    @Environment(AppState.self)
    private var appState

    @Environment(AudioPlaybackController.self)
    private var audioPlayback

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
    @State private var currentAudioUrl: String?
    @State private var currentAudioImageUrl: String?
    @State private var isShowingAudioOpenFailedAlert = false
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
                    if url.absoluteString == AudioEnclosure.shared.PLAY_ACTION_URL {
                        playCurrentAudio()
                        return
                    }
                    if url.absoluteString == AudioEnclosure.shared.ACTION_URL {
                        openCurrentAudio()
                        return
                    }

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
            scripts: ReaderViewScripts(
                fontSize: { readerFontSizeJs(fontSize: Int32($0)) },
                lineHeight: { readerLineHeightJs(step: Int32($0)) },
                lineHeightLabel: { readerLineHeightLabel(step: Int32($0)) },
                stateUpdate: "window.feedflowUpdateAudioState?.(\(isCurrentAudioPlaying ? "true" : "false"));"
            ),
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
                    contentUnavailableMessage: feedFlowStrings.readerModeNoContentMessage,
                    ttsListen: feedFlowStrings.readerModeTtsListen,
                    ttsStop: feedFlowStrings.audioPause,
                    ttsPreparing: feedFlowStrings.readerModeTtsPreparing
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
                onToggleSpeech: toggleCurrentSpeech,
                isPreparingSpeech: audioPlayback.isPreparingSpeech && isCurrentSpeechSelected,
                isSpeechPlaying: audioPlayback.isPlaying && isCurrentSpeechSelected,
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
            },
            isBottomAccessoryVisible: audioPlayback.episode != nil,
            bottomAccessory: {
                AudioPlayerAccessory(
                    openExternal: openAudioExternally,
                    onOpenEpisode: audioPlayback.episode?.itemId != feedItemId ? returnToAudioEpisode : nil
                )
            }
        )
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
        .alert(feedFlowStrings.audioOpenFailed, isPresented: $isShowingAudioOpenFailedAlert) {
            Button(feedFlowStrings.actionDone, role: .cancel) {}
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
                case let .contentNotAvailable(data):
                    self.currentAudioUrl = nil
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
                    self.currentAudioUrl = nil
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
                    self.currentAudioUrl = AudioEnclosure.shared.validatedUrl(
                        url: readerModeData.audioUrl
                    )
                    self.currentAudioImageUrl = readerModeData.audioImageUrl
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

    private func openCurrentAudio() {
        guard let audioUrl = currentAudioUrl,
              let validatedAudioUrl = AudioEnclosure.shared.validatedUrl(url: audioUrl),
              let url = URL(string: validatedAudioUrl) else { return }

        openAudioExternally(url)
    }

    private func openAudioExternally(_ url: URL) {
        audioPlayback.pause()
        let readerItemId = feedItemId

        UIApplication.shared.open(url, options: [:]) { didOpen in
            guard !didOpen else { return }
            Task { @MainActor in
                guard feedItemId == readerItemId else { return }
                isShowingAudioOpenFailedAlert = true
            }
        }
    }

    private func playCurrentAudio() {
        guard let itemId = feedItemId,
              let audioUrl = currentAudioUrl,
              let url = URL(string: audioUrl) else { return }
        let title = feedItemTitle?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        audioPlayback.toggle(AudioEpisode(
            itemId: itemId,
            url: url,
            title: title.isEmpty ? feedFlowStrings.audioEpisodeUntitled : title,
            subtitle: currentSiteName,
            artworkURL: currentAudioImageUrl.flatMap(URL.init(string:))
        ))
    }

    private var isCurrentSpeechSelected: Bool {
        let title = feedItemTitle?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        return audioPlayback.isSpeechFor(
            itemId: feedItemId,
            title: title.isEmpty ? feedFlowStrings.audioEpisodeUntitled : title,
            content: currentContent
        )
    }

    private func toggleCurrentSpeech() {
        guard let itemId = feedItemId,
              let content = currentContent else { return }
        let title = feedItemTitle?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        audioPlayback.toggleSpeech(
            itemId: itemId,
            title: title.isEmpty ? feedFlowStrings.audioEpisodeUntitled : title,
            content: content,
            subtitle: currentSiteName,
            artworkURL: (currentAudioImageUrl ?? currentImageUrl).flatMap(URL.init(string:))
        )
    }

    private var readerAudioBanner: ReaderAudioBanner? {
        guard currentAudioUrl != nil else { return nil }
        return ReaderAudioBanner(
            title: feedItemTitle,
            audioLabel: feedFlowStrings.audioEpisodeLabel,
            openAudioLabel: feedFlowStrings.audioOpenExternal,
            untitledAudioLabel: feedFlowStrings.audioEpisodeUntitled,
            enablePlayback: true,
            subtitle: currentSiteName,
            imageUrl: currentAudioImageUrl,
            playAudioLabel: feedFlowStrings.audioPlay,
            pauseAudioLabel: feedFlowStrings.audioPause
        )
    }

    private var isCurrentAudioPlaying: Bool {
        audioPlayback.episode?.kind == .podcast && audioPlayback.episode?.itemId == feedItemId && audioPlayback.isPlaying
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
            title: feedItemTitle,
            imageUrl: currentImageUrl,
            leadingContent: "",
            siteName: currentSiteName,
            audioBanner: readerAudioBanner
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
        hasher.combine(currentAudioUrl)
        hasher.combine(currentAudioImageUrl)
        return String(hasher.finalize())
    }

    private var themeColors: ReaderThemeColors {
        let codeBlockColors = readerCodeBlockColors(isDarkMode: isDarkMode)
        return ReaderThemeColors(
            textColor: isDarkMode ? "#FFFFFF" : "#000000",
            linkColor: isDarkMode ? "#3B82F6" : "#2563EB",
            backgroundColor: codeBlockColors.backgroundColor,
            borderColor: codeBlockColors.borderColor
        )
    }
}

private extension ReaderModeScreen {
    func returnToAudioEpisode(_ episode: AudioEpisode) {
        Task { @MainActor in
            do {
                let info = try await viewModel.getAudioEpisodeReaderInfo(feedItemId: FeedItemId(id: episode.itemId))
                guard audioPlayback.episode?.itemId == episode.itemId else { return }
                guard let info else {
                    appState.emitGenericError()
                    return
                }
                let openMode = browserSelector.resolvedOpenMode(for: info)
                switch openMode {
                case .fullArticle, .feedContent:
                    viewModel.loadReaderContent(urlInfo: info)
                default:
                    guard let url = URL(string: info.url) else {
                        appState.emitGenericError()
                        return
                    }
                    if openMode == .internalBrowser || browserSelector.openInAppBrowser(),
                       browserSelector.isValidForInAppBrowser(url) {
                        if let onInAppBrowserClick {
                            onInAppBrowserClick(url)
                        } else {
                            appState.openInAppBrowser(url: url)
                        }
                    } else {
                        openURL(browserSelector.getUrlForDefaultBrowser(stringUrl: info.url))
                    }
                }
            } catch is CancellationError {
                return
            } catch {
                appState.emitGenericError()
            }
        }
    }
}
