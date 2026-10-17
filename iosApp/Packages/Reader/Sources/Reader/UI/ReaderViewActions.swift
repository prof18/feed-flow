import SwiftUI

public struct ReaderViewActions {
    public let strings: ReaderViewStrings
    public let onBookmarkToggle: (Bool) -> Void
    public let onArchive: () -> Void
    public let onOpenInBrowser: () -> Void
    public let onComments: (() -> Void)?
    public let onFontSizeMenuToggle: () -> Void
    public let onFontSizeChange: (Double) -> Void
    public let onLineHeightChange: (Double) -> Void
    public let onNavigateToNext: (() -> Void)?
    public let onNavigateToPrevious: (() -> Void)?
    public let onToggleContentSource: (() -> Void)?
    public let onToggleSpeech: (() -> Void)?
    public let isPreparingSpeech: Bool
    public let isSpeechPlaying: Bool
    public let isShowingFeedContent: Bool
    public let hasUrl: Bool
    public let onReportArticle: (() -> Void)?
    public let isSendingReport: Bool

    public init(
        strings: ReaderViewStrings,
        onBookmarkToggle: @escaping (Bool) -> Void,
        onArchive: @escaping () -> Void,
        onOpenInBrowser: @escaping () -> Void,
        onComments: (() -> Void)? = nil,
        onFontSizeMenuToggle: @escaping () -> Void,
        onFontSizeChange: @escaping (Double) -> Void,
        onLineHeightChange: @escaping (Double) -> Void,
        onNavigateToNext: (() -> Void)? = nil,
        onNavigateToPrevious: (() -> Void)? = nil,
        onToggleContentSource: (() -> Void)? = nil,
        onToggleSpeech: (() -> Void)? = nil,
        isPreparingSpeech: Bool = false,
        isSpeechPlaying: Bool = false,
        isShowingFeedContent: Bool = false,
        hasUrl: Bool = true,
        onReportArticle: (() -> Void)? = nil,
        isSendingReport: Bool = false
    ) {
        self.strings = strings
        self.onBookmarkToggle = onBookmarkToggle
        self.onArchive = onArchive
        self.onOpenInBrowser = onOpenInBrowser
        self.onComments = onComments
        self.onFontSizeMenuToggle = onFontSizeMenuToggle
        self.onFontSizeChange = onFontSizeChange
        self.onLineHeightChange = onLineHeightChange
        self.onNavigateToNext = onNavigateToNext
        self.onNavigateToPrevious = onNavigateToPrevious
        self.onToggleContentSource = onToggleContentSource
        self.onToggleSpeech = onToggleSpeech
        self.isPreparingSpeech = isPreparingSpeech
        self.isSpeechPlaying = isSpeechPlaying
        self.isShowingFeedContent = isShowingFeedContent
        self.hasUrl = hasUrl
        self.onReportArticle = onReportArticle
        self.isSendingReport = isSendingReport
    }
}
