import SwiftUI

public struct ReaderViewActions {
    public let strings: ReaderViewStrings
    public let onBookmarkToggle: (Bool) -> Void
    public let onArchive: () -> Void
    public let onOpenInBrowser: () -> Void
    public let onComments: (() -> Void)?
    public let onFontSizeMenuToggle: () -> Void
    public let onFontSizeDecrease: () -> Void
    public let onFontSizeIncrease: () -> Void
    public let onFontSizeChange: (Double) -> Void
    public let onNavigateToNext: (() -> Void)?
    public let onNavigateToPrevious: (() -> Void)?

    public init(
        strings: ReaderViewStrings,
        onBookmarkToggle: @escaping (Bool) -> Void,
        onArchive: @escaping () -> Void,
        onOpenInBrowser: @escaping () -> Void,
        onComments: (() -> Void)? = nil,
        onFontSizeMenuToggle: @escaping () -> Void,
        onFontSizeDecrease: @escaping () -> Void,
        onFontSizeIncrease: @escaping () -> Void,
        onFontSizeChange: @escaping (Double) -> Void,
        onNavigateToNext: (() -> Void)? = nil,
        onNavigateToPrevious: (() -> Void)? = nil
    ) {
        self.strings = strings
        self.onBookmarkToggle = onBookmarkToggle
        self.onArchive = onArchive
        self.onOpenInBrowser = onOpenInBrowser
        self.onComments = onComments
        self.onFontSizeMenuToggle = onFontSizeMenuToggle
        self.onFontSizeDecrease = onFontSizeDecrease
        self.onFontSizeIncrease = onFontSizeIncrease
        self.onFontSizeChange = onFontSizeChange
        self.onNavigateToNext = onNavigateToNext
        self.onNavigateToPrevious = onNavigateToPrevious
    }
}

public struct ReaderView: View {
    @Binding var readerStatus: ReaderStatus
    var options: ReaderViewOptions
    var actions: ReaderViewActions
    var isBookmarked: Bool
    var fontSize: Double
    @Binding var showFontSizeMenu: Bool
    var openInBrowser: (URL) -> Void

    @State private var webContent: WebContent?

    public init(
        readerStatus: Binding<ReaderStatus>,
        options: ReaderViewOptions,
        actions: ReaderViewActions,
        isBookmarked: Bool,
        fontSize: Double,
        showFontSizeMenu: Binding<Bool>,
        openInBrowser: @escaping (URL) -> Void
    ) {
        self._readerStatus = readerStatus
        self.options = options
        self.actions = actions
        self.isBookmarked = isBookmarked
        self.fontSize = fontSize
        self._showFontSizeMenu = showFontSizeMenu
        self.openInBrowser = openInBrowser
    }

    public var body: some View {
        ZStack {
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
                .sheet(isPresented: $showFontSizeMenu) {
                    fontSizeSheet
                        .presentationDetents([.height(200)])
                        .presentationDragIndicator(.visible)
                }

            if !isiOS26OrLater() {
                VStack {
                    Spacer()
                    compactNavigationToolbar
                }
            }
        }
    }

    @ViewBuilder private var content: some View {
        switch readerStatus {
        case .fetching:
            EmptyView()
        case let .failedToExtractContent(url):
            FallbackWebView(
                url: url,
                onLinkClicked: onLinkClicked
            )
        case let .extractedContent(html, baseURL, _):
            ReaderWebView(
                baseURL: baseURL,
                html: html,
                onLinkClicked: onLinkClicked,
                onWebContentReady: { content in
                    webContent = content
                }
            )
        }
    }

    @ViewBuilder private var loader: some View {
        ReaderPlaceholder()
            .opacity(readerStatus == .fetching ? 1 : 0)
            .animation(.default, value: readerStatus == .fetching)
    }

    private func onLinkClicked(_ url: URL) {
        options.onLinkClicked?(url)
    }

    @ToolbarContentBuilder
    private func makeIOS26ToolbarContent() -> some ToolbarContent {
        // Bottom bar - left edge
        if let url = readerStatus.getUrl() {
            ToolbarItem(placement: .bottomBar) {
                Button {
                    openInBrowser(url)
                } label: {
                    Image(systemName: "globe")
                }
            }
        }

        // Bottom bar - right edge (navigation arrows)
        ToolbarItemGroup(placement: .bottomBar) {
            Spacer()

            Button {
                actions.onNavigateToPrevious?()
            } label: {
                Image(systemName: "chevron.left")
            }
            .disabled(actions.onNavigateToPrevious == nil)

            Button {
                actions.onNavigateToNext?()
            } label: {
                Image(systemName: "chevron.right")
            }
            .disabled(actions.onNavigateToNext == nil)
        }

        // Top bar: Share button
        if let url = readerStatus.getUrl() {
            if #available(iOS 26.0, *) {
                ToolbarSpacer(.fixed)
            }

            ToolbarItem(placement: .navigationBarTrailing) {
                ShareLink(
                    item: url,
                    label: {
                        Label(actions.strings.share, systemImage: "square.and.arrow.up")
                    }
                )
            }

            if #available(iOS 26.0, *) {
                ToolbarSpacer(.fixed)
            }
        }

        // Top bar: Menu with other actions
        ToolbarItem(placement: .primaryAction) {
            Menu {
                Button {
                    let newBookmarkState = !isBookmarked
                    actions.onBookmarkToggle(newBookmarkState)
                } label: {
                    Label(
                        isBookmarked ? actions.strings.removeBookmark : actions.strings.addBookmark,
                        systemImage: isBookmarked ? "bookmark.slash" : "bookmark"
                    )
                }

                Button {
                    actions.onArchive()
                } label: {
                    Label(actions.strings.openInArchive, systemImage: "hammer.fill")
                }

                if case .extractedContent = readerStatus,
                   let onComments = actions.onComments {
                    Button {
                        onComments()
                    } label: {
                        Label(actions.strings.openComments, systemImage: "bubble.left")
                    }
                }

                if case .extractedContent = readerStatus {
                    Button {
                        actions.onFontSizeMenuToggle()
                    } label: {
                        Label(actions.strings.fontSize, systemImage: "textformat.size")
                    }
                }
            } label: {
                Image(systemName: "ellipsis.circle")
            }
        }
    }

    @ToolbarContentBuilder
    private func makeLegacyToolbarContent() -> some ToolbarContent {
        // Share button
        if let url = readerStatus.getUrl() {
            ToolbarItem(placement: .navigationBarTrailing) {
                ShareLink(
                    item: url,
                    label: {
                        Label(actions.strings.share, systemImage: "square.and.arrow.up")
                    }
                )
            }
        }

        // Menu with other actions
        ToolbarItem(placement: .primaryAction) {
            Menu {
                Button {
                    let newBookmarkState = !isBookmarked
                    actions.onBookmarkToggle(newBookmarkState)
                } label: {
                    Label(
                        isBookmarked ? actions.strings.removeBookmark : actions.strings.addBookmark,
                        systemImage: isBookmarked ? "bookmark.slash" : "bookmark"
                    )
                }

                Button {
                    actions.onArchive()
                } label: {
                    Label(actions.strings.openInArchive, systemImage: "hammer.fill")
                }

                if case .extractedContent = readerStatus,
                   let onComments = actions.onComments {
                    Button {
                        onComments()
                    } label: {
                        Label(actions.strings.openComments, systemImage: "bubble.left")
                    }
                }

                if case .extractedContent = readerStatus {
                    Button {
                        actions.onFontSizeMenuToggle()
                    } label: {
                        Label(actions.strings.fontSize, systemImage: "textformat.size")
                    }
                }
            } label: {
                Image(systemName: "ellipsis.circle")
            }
        }
    }

    @ViewBuilder
    private var fontSizeSheet: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text(actions.strings.fontSize)
                .font(.headline)

            HStack(spacing: 16) {
                Button {
                    let newFontSize = fontSize - 1.0
                    updateFontSizeWithJS(newFontSize)
                    actions.onFontSizeDecrease()
                } label: {
                    Image(systemName: "minus.circle.fill")
                        .font(.title2)
                }
                .disabled(fontSize <= 12)

                Slider(
                    value: Binding(
                        get: { fontSize },
                        set: { newValue in
                            updateFontSizeWithJS(newValue)
                            actions.onFontSizeChange(newValue)
                        }
                    ),
                    in: 12 ... 40
                )

                Button {
                    let newFontSize = fontSize + 1.0
                    updateFontSizeWithJS(newFontSize)
                    actions.onFontSizeIncrease()
                } label: {
                    Image(systemName: "plus.circle.fill")
                        .font(.title2)
                }
                .disabled(fontSize >= 40)
            }

            Text("\(Int(fontSize))px")
                .font(.subheadline)
                .foregroundStyle(.secondary)
        }
        .padding()
    }

    private func updateFontSizeWithJS(_ newFontSize: Double) {
        guard let webContent = webContent else { return }
        let script = """
            document.getElementById("container").style.fontSize = "\(Int(newFontSize))" + "px";
            document.getElementById("container").style.lineHeight = "1.5em";
        """
        webContent.evaluateJavaScript(script)
    }

    @ViewBuilder
    private var compactNavigationToolbar: some View {
        HStack(spacing: 0) {
            // Open in browser - left edge
            if let url = readerStatus.getUrl() {
                Button {
                    openInBrowser(url)
                } label: {
                    Image(systemName: "globe")
                        .font(.title3)
                        .foregroundStyle(.primary)
                        .frame(width: 50, height: 44)
                }
                .background(.regularMaterial)
                .clipShape(Capsule())
                .shadow(radius: 8)
            }

            Spacer()

            // Navigation arrows - right edge
            HStack(spacing: 0) {
                Button {
                    actions.onNavigateToPrevious?()
                } label: {
                    Image(systemName: "chevron.left")
                        .font(.title3)
                        .foregroundStyle(.primary)
                        .frame(width: 50, height: 44)
                }
                .disabled(actions.onNavigateToPrevious == nil)

                Button {
                    actions.onNavigateToNext?()
                } label: {
                    Image(systemName: "chevron.right")
                        .font(.title3)
                        .foregroundStyle(.primary)
                        .frame(width: 50, height: 44)
                }
                .disabled(actions.onNavigateToNext == nil)
            }
            .background(.regularMaterial)
            .clipShape(Capsule())
            .shadow(radius: 8)
        }
        .padding(.horizontal, 16)
        .padding(.bottom, 16)
    }
}
