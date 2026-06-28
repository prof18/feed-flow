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
        onNavigateToPrevious: (() -> Void)? = nil
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
    }
}

public struct ReaderView: View {
    @Binding var readerStatus: ReaderStatus
    var options: ReaderViewOptions
    var themeColors: ReaderThemeColors
    var actions: ReaderViewActions
    var isBookmarked: Bool
    var fontSize: Double
    var lineHeight: Double
    var defaultFontSize: Double
    var defaultLineHeight: Double
    @Binding var showFontSizeMenu: Bool
    var openInBrowser: (URL) -> Void

    @State private var webContent: WebContent?

    public init(
        readerStatus: Binding<ReaderStatus>,
        options: ReaderViewOptions,
        themeColors: ReaderThemeColors,
        actions: ReaderViewActions,
        isBookmarked: Bool,
        fontSize: Double,
        lineHeight: Double,
        defaultFontSize: Double,
        defaultLineHeight: Double,
        showFontSizeMenu: Binding<Bool>,
        openInBrowser: @escaping (URL) -> Void
    ) {
        self._readerStatus = readerStatus
        self.options = options
        self.themeColors = themeColors
        self.actions = actions
        self.isBookmarked = isBookmarked
        self.fontSize = fontSize
        self.lineHeight = lineHeight
        self.defaultFontSize = defaultFontSize
        self.defaultLineHeight = defaultLineHeight
        self._showFontSizeMenu = showFontSizeMenu
        self.openInBrowser = openInBrowser
    }

    public var body: some View {
        ZStack {
            Color(ReaderTheme.background)
                .overlay(content)
                .overlay(loader)
                .navigationBarTitleDisplayMode(.inline)
                .onChange(of: themeColors) { _, newValue in
                    applyThemeWithJS(newValue)
                }
                .toolbar {
                    if isiOS26OrLater() {
                        makeIOS26ToolbarContent()
                    } else {
                        makeLegacyToolbarContent()
                    }
                }
                .sheet(isPresented: $showFontSizeMenu) {
                    fontSizeSheet
                        .presentationDetents([.medium])
                        .presentationBackground(Color(.secondarySystemBackground))
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
                onImageClicked: options.onImageClicked,
                onWebContentReady: { content in
                    webContent = content
                    applyThemeWithJS(themeColors)
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

    private func applyThemeWithJS(_ colors: ReaderThemeColors) {
        guard let webContent = webContent else { return }
        let script = """
            document.documentElement.style.setProperty("--reader-text", "\(colors.textColor)");
            document.documentElement.style.setProperty("--reader-link", "\(colors.linkColor)");
            document.documentElement.style.setProperty("--reader-bg", "\(colors.backgroundColor)");
            document.documentElement.style.setProperty("--reader-border", "\(colors.borderColor)");
        """
        webContent.evaluateJavaScript(script)
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
                .accessibilityIdentifier(ReaderAccessibilityIdentifiers.browserButton)
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
            .accessibilityIdentifier(ReaderAccessibilityIdentifiers.previousButton)

            Button {
                actions.onNavigateToNext?()
            } label: {
                Image(systemName: "chevron.right")
            }
            .disabled(actions.onNavigateToNext == nil)
            .accessibilityIdentifier(ReaderAccessibilityIdentifiers.nextButton)
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
                .accessibilityIdentifier(ReaderAccessibilityIdentifiers.bookmarkButton)

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
                        Label(actions.strings.textSettings, systemImage: "textformat.size")
                    }
                    .accessibilityIdentifier(ReaderAccessibilityIdentifiers.fontSizeButton)
                }
            } label: {
                Image(systemName: "ellipsis.circle")
                    .accessibilityIdentifier(ReaderAccessibilityIdentifiers.moreMenuButton)
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
                .accessibilityIdentifier(ReaderAccessibilityIdentifiers.bookmarkButton)

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
                        Label(actions.strings.textSettings, systemImage: "textformat.size")
                    }
                    .accessibilityIdentifier(ReaderAccessibilityIdentifiers.fontSizeButton)
                }
            } label: {
                Image(systemName: "ellipsis.circle")
                    .accessibilityIdentifier(ReaderAccessibilityIdentifiers.moreMenuButton)
            }
        }
    }

    @ViewBuilder
    private var fontSizeSheet: some View {
        NavigationStack {
            VStack(spacing: 0) {
                VStack(alignment: .leading, spacing: 22) {
                    textSettingSliderRow(
                        title: actions.strings.fontSize,
                        valueLabel: fontSizeValueLabel(fontSize),
                        valueAccessibilityIdentifier: ReaderAccessibilityIdentifiers.fontSizeValueLabel,
                        value: fontSize,
                        range: 12 ... 40,
                        step: 1,
                        onValueChange: {
                            updateFontSizeWithJS($0)
                            actions.onFontSizeChange($0)
                        }
                    )

                    Divider()

                    textSettingSliderRow(
                        title: actions.strings.lineHeight,
                        valueLabel: lineHeightValueLabel(lineHeight),
                        valueAccessibilityIdentifier: ReaderAccessibilityIdentifiers.lineHeightValueLabel,
                        value: lineHeight,
                        range: 0 ... 15,
                        step: 1,
                        onValueChange: {
                            updateLineHeightWithJS($0)
                            actions.onLineHeightChange($0)
                        }
                    )

                    HStack {
                        Spacer()
                        resetTextSettingsButton
                    }
                }
                .padding(.horizontal, 24)
                .padding(.top, 56)

                Spacer(minLength: 0)
            }
            .background(Color(.secondarySystemBackground))
            .navigationTitle(actions.strings.textSettings)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button {
                        showFontSizeMenu = false
                    } label: {
                        Text(actions.strings.done).bold()
                    }
                }
            }
        }
        .accessibilityIdentifier(ReaderAccessibilityIdentifiers.textSettingsSheet)
    }

    private var resetTextSettingsButton: some View {
        let isDisabled = fontSize == defaultFontSize && lineHeight == defaultLineHeight
        return Button {
            updateFontSizeWithJS(defaultFontSize)
            updateLineHeightWithJS(defaultLineHeight)
            actions.onFontSizeChange(defaultFontSize)
            actions.onLineHeightChange(defaultLineHeight)
        } label: {
            Text(actions.strings.resetToDefault)
                .font(.body)
        }
        .buttonStyle(.plain)
        .foregroundStyle(isDisabled ? Color.secondary : Color.accentColor)
        .disabled(isDisabled)
        .accessibilityIdentifier(ReaderAccessibilityIdentifiers.textSettingsResetButton)
    }

    @ViewBuilder
    private func textSettingSliderRow(
        title: String,
        valueLabel: String,
        valueAccessibilityIdentifier: String,
        value: Double,
        range: ClosedRange<Double>,
        step: Double?,
        onValueChange: @escaping (Double) -> Void
    ) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text(title)
                Spacer()
                Text(valueLabel)
                    .font(.body.weight(.semibold))
                    .foregroundStyle(.secondary)
                    .accessibilityIdentifier(valueAccessibilityIdentifier)
            }

            if let step {
                Slider(
                    value: Binding(
                        get: { value },
                        set: onValueChange
                    ),
                    in: range,
                    step: step
                )
            } else {
                Slider(
                    value: Binding(
                        get: { value },
                        set: onValueChange
                    ),
                    in: range
                )
            }
        }
        .padding(.vertical, 4)
    }

    private func fontSizeValueLabel(_ fontSize: Double) -> String {
        "\(Int(fontSize))"
    }

    private func lineHeightValueLabel(_ lineHeight: Double) -> String {
        let tenths = 15 + Int(lineHeight)
        return "\(tenths / 10).\(tenths % 10)"
    }

    private func updateFontSizeWithJS(_ newFontSize: Double) {
        guard let webContent = webContent else { return }
        let script = """
            document.getElementById("container").style.fontSize = "\(Int(newFontSize))" + "px";
        """
        webContent.evaluateJavaScript(script)
    }

    // Mirror of Kotlin readerLineHeightToCss + readerLineHeightJs - keep in sync.
    private func updateLineHeightWithJS(_ lineHeight: Double) {
        guard let webContent = webContent else { return }
        let tenths = 15 + Int(lineHeight)
        let lineHeightCss = "\(tenths / 10).\(tenths % 10)"
        let script = """
            (function() {
              var styleId = "__feedflow_line_height_style";
              var style = document.getElementById(styleId);
              if (!style) {
                style = document.createElement("style");
                style.id = styleId;
                document.head.appendChild(style);
              }
              style.textContent = "body, #__content { line-height: \(lineHeightCss); }";
            })();
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
                .accessibilityIdentifier(ReaderAccessibilityIdentifiers.browserButton)
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
                .accessibilityIdentifier(ReaderAccessibilityIdentifiers.previousButton)

                Button {
                    actions.onNavigateToNext?()
                } label: {
                    Image(systemName: "chevron.right")
                        .font(.title3)
                        .foregroundStyle(.primary)
                        .frame(width: 50, height: 44)
                }
                .disabled(actions.onNavigateToNext == nil)
                .accessibilityIdentifier(ReaderAccessibilityIdentifiers.nextButton)
            }
            .background(.regularMaterial)
            .clipShape(Capsule())
            .shadow(radius: 8)
        }
        .padding(.horizontal, 16)
        .padding(.bottom, 16)
    }
}
