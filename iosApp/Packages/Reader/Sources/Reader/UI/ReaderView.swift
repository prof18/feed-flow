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
    @Binding var readerStatus: ReaderStatus
    var options: ReaderViewOptions
    var actions: ReaderViewActions
    var isBookmarked: Bool
    var fontSize: Double
    var showFontSizeMenu: Bool
    var openInBrowser: (URL) -> Void

    @State private var webContent: WebContent?

    public init(
        readerStatus: Binding<ReaderStatus>,
        options: ReaderViewOptions,
        actions: ReaderViewActions,
        isBookmarked: Bool,
        fontSize: Double,
        showFontSizeMenu: Bool,
        openInBrowser: @escaping (URL) -> Void
    ) {
        self._readerStatus = readerStatus
        self.options = options
        self.actions = actions
        self.isBookmarked = isBookmarked
        self.fontSize = fontSize
        self.showFontSizeMenu = showFontSizeMenu
        self.openInBrowser = openInBrowser
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

            if let url = readerStatus.getUrl() {
                ShareLink(
                    item: url,
                    label: {
                        Label("Share", systemImage: "square.and.arrow.up")
                    }
                )
            }

            Button {
                actions.onArchive()
            } label: {
                Image(systemName: "hammer.fill")
            }
        }

        if let url = readerStatus.getUrl() {
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
                    openInBrowser(url)
                } label: {
                    Image(systemName: "globe")
                }
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
            fontSizeMenu
        }

        if let url = readerStatus.getUrl() {
            ToolbarItem {
                Button {
                    openInBrowser(url)
                } label: {
                    Image(systemName: "globe")
                }
            }

            ToolbarItem {
                Menu {
                    Button {
                        let newBookmarkState = !isBookmarked
                        actions.onBookmarkToggle(newBookmarkState)
                    } label: {
                        Label(
                            isBookmarked ? "Remove Bookmark" : "Add Bookmark",
                            systemImage: isBookmarked ? "bookmark.slash" : "bookmark"
                        )
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
                        Label("Open in Archive", systemImage: "hammer.fill")
                    }

                    if let onComments = actions.onComments {
                        Button {
                            onComments()
                        } label: {
                            Label("Open Comments", systemImage: "bubble.left")
                        }
                    }
                } label: {
                    Image(systemName: "ellipsis.circle")
                }
            }
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
                        let newFontSize = fontSize - 1.0
                        updateFontSizeWithJS(newFontSize)
                        actions.onFontSizeDecrease()
                    } label: {
                        Image(systemName: "minus")
                    }

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
                        Image(systemName: "plus")
                    }
                }
            }
            .frame(width: 250, height: 100)
            .padding(.horizontal, 16)
            .presentationCompactAdaptation((.popover))
        }
    }

    private func updateFontSizeWithJS(_ newFontSize: Double) {
        guard let webContent = webContent else { return }
        let script = """
            document.getElementById("container").style.fontSize = "\(Int(newFontSize))" + "px";
            document.getElementById("container").style.lineHeight = "1.5em";
        """
        webContent.evaluateJavaScript(script)
    }
}
