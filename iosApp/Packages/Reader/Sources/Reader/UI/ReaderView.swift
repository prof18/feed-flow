import SwiftUI

public struct ReaderViewActions {
    public let onBookmarkToggle: (Bool) -> Void
    public let onArchive: () -> Void
    public let onOpenInBrowser: () -> Void
    public let onComments: (() -> Void)?
    public let onFontSizeMenuToggle: () -> Void
    public let onFontSizeDecrease: () -> Void
    public let onFontSizeIncrease: () -> Void
    public let onFontSizeChange: (Double) -> Void

    public init(
        onBookmarkToggle: @escaping (Bool) -> Void,
        onArchive: @escaping () -> Void,
        onOpenInBrowser: @escaping () -> Void,
        onComments: (() -> Void)? = nil,
        onFontSizeMenuToggle: @escaping () -> Void,
        onFontSizeDecrease: @escaping () -> Void,
        onFontSizeIncrease: @escaping () -> Void,
        onFontSizeChange: @escaping (Double) -> Void
    ) {
        self.onBookmarkToggle = onBookmarkToggle
        self.onArchive = onArchive
        self.onOpenInBrowser = onOpenInBrowser
        self.onComments = onComments
        self.onFontSizeMenuToggle = onFontSizeMenuToggle
        self.onFontSizeDecrease = onFontSizeDecrease
        self.onFontSizeIncrease = onFontSizeIncrease
        self.onFontSizeChange = onFontSizeChange
    }
}

public struct ReaderView: View {
    var url: URL
    var options: ReaderViewOptions
    var actions: ReaderViewActions
    var isBookmarked: Bool
    var fontSize: Double
    var showFontSizeMenu: Bool

    @State private var status = ReaderStatus.fetching
    @State private var titleFromFallbackWebView: String?

    private var title: String? {
        switch status {
        case .fetching:
            return nil
        case .failedToExtractContent:
            return titleFromFallbackWebView
        case let .extractedContent(_, _, title):
            return title
        }
    }

    public init(
        url: URL,
        options: ReaderViewOptions,
        actions: ReaderViewActions,
        isBookmarked: Bool,
        fontSize: Double,
        showFontSizeMenu: Bool
    ) {
        self.url = url
        self.options = options
        self.actions = actions
        self.isBookmarked = isBookmarked
        self.fontSize = fontSize
        self.showFontSizeMenu = showFontSizeMenu
    }

    public var body: some View {
        Color(ReaderTheme.background)
            .overlay(content)
            .overlay(loader)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                if isiOS26OrLater() {
                    makeIOS26ToolbarContent()
                } else {
                    makeLegacyToolbarContent()
                }
            }
            .task {
                do {
                    let result = try await Reader.fetchAndExtractContent(
                        fromURL: url,
                        additionalCSS: options.additionalCSS
                    )
                    self.status = .extractedContent(
                        html: result.styledHTML,
                        baseURL: result.baseURL,
                        title: result.title
                    )
                } catch {
                    status = .failedToExtractContent
                }
            }
    }

    @ViewBuilder private var content: some View {
        switch status {
        case .fetching:
            EmptyView()
        case .failedToExtractContent:
            FallbackWebView(
                url: url,
                onLinkClicked: onLinkClicked,
                title: $titleFromFallbackWebView
            )
        case let .extractedContent(html, baseURL, _):
            ReaderWebView(baseURL: baseURL, html: html, onLinkClicked: onLinkClicked)
        }
    }

    @ViewBuilder private var loader: some View {
        ReaderPlaceholder()
            .opacity(status == .fetching ? 1 : 0)
            .animation(.default, value: status == .fetching)
    }

    private func onLinkClicked(_ url: URL) {
        if url == .exitReaderModeLink {
            status = .failedToExtractContent
        } else {
            options.onLinkClicked?(url)
        }
    }

    @ToolbarContentBuilder
    private func makeIOS26ToolbarContent() -> some ToolbarContent {
        ToolbarItemGroup(placement: .bottomBar) {
            Button {
                let newBookmarkState = !isBookmarked
                actions.onBookmarkToggle(newBookmarkState)
            } label: {
                if isBookmarked {
                    Image(systemName: "bookmark.slash")
                } else {
                    Image(systemName: "bookmark")
                }
            }

            ShareLink(
                item: url,
                label: {
                    Label("Share", systemImage: "square.and.arrow.up")
                }
            )

            Button {
                actions.onArchive()
            } label: {
                Image(systemName: "hammer.fill")
            }
        }

        if let onComments = actions.onComments {
            ToolbarItem {
                Button {
                    onComments()
                } label: {
                    Image(systemName: "bubble.left")
                }
            }
        }

        ToolbarItem {
            Button {
                actions.onOpenInBrowser()
            } label: {
                Image(systemName: "globe")
            }
        }

        if #available(iOS 26.0, *) {
            ToolbarSpacer(.fixed)
        }

        ToolbarItem {
            fontSizeMenu
        }
    }

    @ToolbarContentBuilder
    private func makeLegacyToolbarContent() -> some ToolbarContent {
        ToolbarItem {
            Button {
                let newBookmarkState = !isBookmarked
                actions.onBookmarkToggle(newBookmarkState)
            } label: {
                if isBookmarked {
                    Image(systemName: "bookmark.slash")
                } else {
                    Image(systemName: "bookmark")
                }
            }
        }

        ToolbarItem {
            ShareLink(
                item: url,
                label: {
                    Label("Share", systemImage: "square.and.arrow.up")
                }
            )
        }

        ToolbarItem {
            Button {
                actions.onArchive()
            } label: {
                Image(systemName: "hammer.fill")
            }
        }

        if let onComments = actions.onComments {
            ToolbarItem {
                Button {
                    onComments()
                } label: {
                    Image(systemName: "bubble.left")
                }
            }
        }

        ToolbarItem {
            Button {
                actions.onOpenInBrowser()
            } label: {
                Image(systemName: "globe")
            }
        }

        ToolbarItem {
            fontSizeMenu
        }
    }

    @ViewBuilder
    private var fontSizeMenu: some View {
        Button {
            actions.onFontSizeMenuToggle()
        } label: {
            Image(systemName: "textformat.size")
        }
        .font(.title3)
        .popover(isPresented: .constant(showFontSizeMenu)) {
            VStack(alignment: .leading) {
                Text("Font Size")

                HStack {
                    Button {
                        actions.onFontSizeDecrease()
                    } label: {
                        Image(systemName: "minus")
                    }

                    Slider(
                        value: .constant(fontSize),
                        in: 12 ... 40,
                        onEditingChanged: { isEditing in
                            if !isEditing {
                                actions.onFontSizeChange(fontSize)
                            }
                        }
                    )

                    Button {
                        actions.onFontSizeIncrease()
                    } label: {
                        Image(systemName: "plus")
                    }
                }
            }
            .frame(width: 250, height: 100)
            .padding(.horizontal, 16)
            .presentationCompactAdaptation((.popover))
        }
    }
}
